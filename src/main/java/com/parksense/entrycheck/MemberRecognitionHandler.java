package com.parksense.entrycheck;

import com.parksense.members.MemberRegistry;

/**
 * Never rejects: recognises member plates so the exit lane can settle
 * them under the pass tariff without a payment step.
 */
public final class MemberRecognitionHandler extends EntryRuleHandler {

    private final MemberRegistry members;

    public MemberRecognitionHandler(MemberRegistry members) {
        this.members = members;
    }

    @Override
    public String ruleName() {
        return "Member recognition";
    }

    @Override
    protected boolean check(EntryContext context) {
        if (members.hasValidPass(context.vehicle().plateNo())) {
            context.markMemberPlate();
            context.passed(ruleName(), "valid pass on file");
        } else {
            context.passed(ruleName(), "casual driver");
        }
        return true;
    }
}
