package com.dmaldonado.codex_latinus.model.ast;

import java.util.List;

/**
 * Root of the Abstract Syntax Tree hierarchy (Composite pattern).
 *
 * Every node knows its position in the source file so errors can be reported
 * as line:column, and accepts a visitor. getLabel() and getChildren() let the
 * AST graph and any generic traversal work on any node without knowing its
 * concrete type.
 */
public abstract class AstNode
{
    private final int line;
    private final int column;

    protected AstNode(int line, int column)
    {
        this.line   = line;
        this.column = column;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    /** Entry point of the Visitor pattern. */
    public abstract <T> T accept(AstVisitor<T> visitor);

    /** Text drawn inside the node when the AST is graphed. */
    public abstract String getLabel();

    /** Direct children, in source order, for generic traversals. */
    public abstract List<AstNode> getChildren();

    @Override
    public String toString()
    {
        return getLabel();
    }
}
