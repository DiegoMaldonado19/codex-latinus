package com.dmaldonado.codex_latinus.model.ast;

import com.dmaldonado.codex_latinus.model.types.DataType;

/**
 * Node that produces a value.
 *
 * Marker base class: it separates what can appear on the right hand side of an
 * assignment from what cannot.
 *
 * The two mutable fields are filled in by the semantic analyzer, never by the
 * AST builder:
 *   - computedType: the inferred type, so later phases do not redo the work and
 *     the AST graph can show it.
 *   - structName: which structura this expression refers to when its type is
 *     ESTRUCTURA. It is the only channel that makes a chain like
 *     mi_selva.animales[1].nombre resolvable, because DataType.ESTRUCTURA alone
 *     does not say WHICH structure.
 */
public abstract class Expression extends AstNode
{
    private DataType computedType = DataType.ERROR;
    private String   structName;

    protected Expression(int line, int column)
    {
        super(line, column);
    }

    public DataType getComputedType()
    {
        return computedType;
    }

    public void setComputedType(DataType computedType)
    {
        this.computedType = computedType;
    }

    public String getStructName()
    {
        return structName;
    }

    public void setStructName(String structName)
    {
        this.structName = structName;
    }
}
