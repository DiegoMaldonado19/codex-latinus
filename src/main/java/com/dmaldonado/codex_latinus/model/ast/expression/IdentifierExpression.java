package com.dmaldonado.codex_latinus.model.ast.expression;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.List;

/** Use of a variable by name. This is what the PigLatin rules rename. */
public class IdentifierExpression extends Expression
{
    private final String name;

    public IdentifierExpression(String name, int line, int column)
    {
        super(line, column);
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitIdentifierExpression(this);
    }

    @Override
    public String getLabel()
    {
        return name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
