package com.dmaldonado.codex_latinus.model.ast;

/**
 * Node that produces a value.
 *
 * Marker base class: it separates what can appear on the right hand side of an
 * assignment from what cannot. The inferred type is filled in later by the
 * semantic analyzer, not here.
 */
public abstract class Expression extends AstNode
{
    protected Expression(int line, int column)
    {
        super(line, column);
    }
}
