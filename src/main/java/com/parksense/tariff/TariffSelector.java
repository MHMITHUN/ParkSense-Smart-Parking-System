package com.parksense.tariff;

import com.parksense.tariff.strategy.DailyCapTariff;
import com.parksense.tariff.strategy.EarlyBirdTariff;
import com.parksense.tariff.strategy.EventSurgeTariff;
import com.parksense.tariff.strategy.HourlyTariff;
import com.parksense.tariff.strategy.MemberPassTariff;
import com.parksense.store.TariffStore;
import com.parksense.tariff.strategy.TariffStrategy;

import java.util.Optional;

/**
 * Chooses the strategy for an exit (the Context of the GoF Strategy
 * pattern). Priority: member pass → event surge → early bird → capped
 * hourly → plain hourly. The first matching <em>active</em> plan wins, so
 * operators turn a scheme on or off simply by activating its plan.
 */
public final class TariffSelector {

    private final HourlyTariff hourly = new HourlyTariff();
    private final DailyCapTariff dailyCap = new DailyCapTariff();
    private final EarlyBirdTariff earlyBird = new EarlyBirdTariff();
    private final EventSurgeTariff surge = new EventSurgeTariff();
    private final MemberPassTariff memberPass = new MemberPassTariff();

    /** What the selector decided, and why — recorded on the ticket. */
    public record Selected(TariffPlan plan, TariffStrategy strategy, String reason) {
    }

    private final TariffStore store;

    public TariffSelector(TariffStore store) {
        this.store = store;
    }

    public Selected select(FeeRequest request, boolean memberWithValidPass) {
        if (memberWithValidPass) {
            Optional<TariffPlan> plan = store.activeByKind(TariffKind.MEMBER_PASS);
            if (plan.isPresent()) {
                return new Selected(plan.get(), memberPass, "valid member pass on file");
            }
        }
        Optional<TariffPlan> surgePlan = store.activeByKind(TariffKind.EVENT_SURGE);
        if (surgePlan.isPresent()) {
            return new Selected(surgePlan.get(), surge, "event surge window active");
        }
        Optional<TariffPlan> earlyPlan = store.activeByKind(TariffKind.EARLY_BIRD);
        if (earlyPlan.isPresent() && earlyBird.eligible(earlyPlan.get(), request)) {
            return new Selected(earlyPlan.get(), earlyBird, "early-bird window matched");
        }
        Optional<TariffPlan> capPlan = store.activeByKind(TariffKind.DAILY_CAP);
        if (capPlan.isPresent()) {
            return new Selected(capPlan.get(), dailyCap, "default capped hourly plan");
        }
        Optional<TariffPlan> hourlyPlan = store.activeByKind(TariffKind.HOURLY);
        if (hourlyPlan.isPresent()) {
            return new Selected(hourlyPlan.get(), hourly, "default hourly plan");
        }
        throw new IllegalStateException("No active tariff plan — configure one in Tariff Admin");
    }
}
