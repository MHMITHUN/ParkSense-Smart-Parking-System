package com.parksense.tariff.strategy;

import com.parksense.common.Money;
import com.parksense.tariff.FeeRequest;
import com.parksense.tariff.TariffKind;
import com.parksense.tariff.TariffPlan;

import java.math.BigDecimal;

/**
 * Monthly pass holders pay nothing per visit — the subscription already
 * did. This strategy exists so the exit pipeline can treat members and
 * casual drivers through exactly the same code path.
 */
public final class MemberPassTariff implements TariffStrategy {

    @Override
    public TariffKind kind() {
        return TariffKind.MEMBER_PASS;
    }

    @Override
    public BigDecimal compute(TariffPlan plan, FeeRequest request) {
        return Money.ZERO;
    }

    @Override
    public String explain(TariffPlan plan, FeeRequest request) {
        return "Monthly member pass (" + plan.name() + ")";
    }
}
