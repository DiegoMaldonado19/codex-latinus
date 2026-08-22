package com.dmaldonado.codex_latinus.model.ast.expression;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/** calcularPoder(10, 0.5) */
public class FunctionCallExpression extends Expression
{
    private final String           name;
    private final List<Expression> arguments;

    public FunctionCallExpression(String name, List<Expression> arguments, int line, int column)
    {
        super(line, column);
        this.name      = name;
        this.arguments = arguments;
    }

    public String getName()
    {
        return name;
    }

    public List<Expression> getArguments()
    {
        return arguments;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitFunctionCallExpression(this);
    }

    @Override
    public String getLabel()
    {
        return name + "()";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(arguments);
    }
}
