package com.parksense.entrycheck;

/**
 * GoF Chain of Responsibility. Entry authorisation is an ordered list of
 * independent rules; each handler checks one and either passes the request
 * to the next or stops the chain with a reason the lane display can show
 * verbatim. Wiring order is decided once, where the chain is assembled.
 */
public abstract class EntryRuleHandler {

    private EntryRuleHandler next;

    /** Link another handler after this one; returns the new tail for chaining. */
    public EntryRuleHandler linkWith(EntryRuleHandler nextHandler) {
        this.next = nextHandler;
        return nextHandler;
    }

    /** Run this handler, then the rest of the chain. */
    public boolean handle(EntryContext context) {
        if (!check(context)) {
            return false;
        }
        return next == null || next.handle(context);
    }

    /** The rule's name for the trace. */
    public abstract String ruleName();

    /** @return true to continue the chain, false to reject. */
    protected abstract boolean check(EntryContext context);
}
