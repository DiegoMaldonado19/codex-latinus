package com.dmaldonado.codex_latinus.model.stack;

/**
 * Operations shown in the process stack panel.
 *
 * The statement asks for "shift, replace, accept", which is LR vocabulary, and
 * ANTLR4 is LL(*). The engineer confirmed the simulation is what is expected
 * (Telegram 9/08 22:35), so these five names are what the parse tree walk is
 * translated into, not what ANTLR actually does internally.
 */
public enum StepAction
{
    SHIFT("SHIFT"),
    PUSH("PUSH (regla activa)"),
    REDUCE("REDUCE / REPLACE"),
    ACCEPT("ACCEPT"),
    ERROR("ERROR");

    private final String label;

    StepAction(String label)
    {
        this.label = label;
    }

    /** What a ListView cell shows without any extra formatting. */
    @Override
    public String toString()
    {
        return label;
    }
}
