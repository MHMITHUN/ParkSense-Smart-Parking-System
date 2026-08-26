package com.parksense.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Full-stack flow through the real HTTP surface: login → entry → add-on →
 * pay → exit → map freed → revenue includes the payment.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EndToEndFlowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    private String adminToken() throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk()).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    void unauthenticatedApiCallIsRejected() throws Exception {
        mvc.perform(get("/api/map")).andExpect(status().isUnauthorized());
    }

    @Test
    void badLoginIsRejected() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void entryToExitToRevenueRoundTrip() throws Exception {
        String token = adminToken();
        var om = json;

        // free slots before
        int freeBefore = om.readTree(mvc.perform(get("/api/map/summary").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("freeSlots").asInt();

        // entry
        MvcResult entryRes = mvc.perform(post("/api/gates/GATE-IN-1/entry")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"plate\":\"E2E-100\",\"vehicleType\":\"CAR\",\"accessible\":false}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode entry = om.readTree(entryRes.getResponse().getContentAsString());
        assertTrue(entry.get("accepted").asBoolean());
        String ticketNo = entry.get("ticketNo").asText();
        String slot = entry.get("slotCode").asText();
        assertFalse(ticketNo.isBlank());

        // duplicate entry refused
        mvc.perform(post("/api/gates/GATE-IN-1/entry")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"plate\":\"E2E-100\",\"vehicleType\":\"CAR\",\"accessible\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accepted").value(false));

        // map shows one fewer free slot
        int freeAfter = om.readTree(mvc.perform(get("/api/map/summary").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("freeSlots").asInt();
        assertEquals(freeBefore - 1, freeAfter);

        // add-on then pay
        mvc.perform(post("/api/tickets/{t}/addons", ticketNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"code\":\"CAR_WASH\"}"))
                .andExpect(status().isOk());
        MvcResult payRes = mvc.perform(post("/api/tickets/{t}/pay", ticketNo)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"method\":\"CASH\",\"tendered\":500}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode receipt = om.readTree(payRes.getResponse().getContentAsString());
        assertTrue(receipt.get("total").asDouble() > 0);
        assertTrue(receipt.get("change").asDouble() >= 0);
        assertNotNull(receipt.get("graceUntil"));

        // exit
        MvcResult exitRes = mvc.perform(post("/api/gates/GATE-OUT-1/exit")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reference\":\"E2E-100\"}"))
                .andExpect(status().isOk()).andReturn();
        JsonNode exit = om.readTree(exitRes.getResponse().getContentAsString());
        assertTrue(exit.get("allowed").asBoolean(), exit.toString());

        // slot is free again
        int freeFinal = om.readTree(mvc.perform(get("/api/map/summary").header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("freeSlots").asInt();
        assertEquals(freeBefore, freeFinal);

        // revenue report has rows and includes today
        MvcResult revRes = mvc.perform(get("/api/reports/revenue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        JsonNode revenue = om.readTree(revRes.getResponse().getContentAsString());
        assertTrue(revenue.get("rows").size() >= 1);
        assertTrue(revenue.get("seriesValues").size() >= 1);

        // patterns endpoint lists all 16
        MvcResult patRes = mvc.perform(get("/api/system/patterns")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        assertEquals(16, om.readTree(patRes.getResponse().getContentAsString()).size());
    }

    @Test
    void operatorCannotForceBarrier() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"operator\",\"password\":\"operator123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();

        mvc.perform(post("/api/control/gates/GATE-IN-1/force-open")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        // audit trail recorded the denial (admin can read)
        String admin = adminToken();
        MvcResult audit = mvc.perform(get("/api/system/audit?limit=5")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk()).andReturn();
        assertTrue(audit.getResponse().getContentAsString().contains("FORCE_OPEN"));
    }

    @Test
    void operatorCannotVoidTickets() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"operator\",\"password\":\"operator123\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(post("/api/tickets/PS-000000-00001/void")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"reason\":\"test\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void seedShapeIsRealistic() throws Exception {
        String token = adminToken();
        JsonNode map = json.readTree(mvc.perform(get("/api/map")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        assertEquals(132, map.get("totalSlots").asInt());
        assertTrue(map.get("freeSlots").asInt() > 20);
        assertTrue(map.get("freeSlots").asInt() < 132);
    }
}
