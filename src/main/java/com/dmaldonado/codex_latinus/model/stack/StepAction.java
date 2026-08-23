package com.dmaldonado.codex_latinus.model.stack;

/**
 * Operaciones del panel de la pila. El enunciado pide vocabulario LR (shift,
 * replace, accept) y ANTLR4 es LL(*): el ingeniero confirmo que la simulacion
 * es lo esperado (Telegram 9/08 22:35).
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
