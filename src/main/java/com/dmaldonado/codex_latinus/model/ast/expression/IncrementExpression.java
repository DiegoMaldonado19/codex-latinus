package com.dmaldonado.codex_latinus.model.ast.expression;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.List;

/** ++i and i++ used inside an expression, where the value is consumed. */
public class IncrementExpression extends Expression
{
    private final Expression target;
    private final String     operator;
    private final boolean    prefix;

    public IncrementExpression(Expression target, String operator, boolean prefix,
                               int line, int column)
    {
        super(line, column);
        this.target   = target;
        this.operator = operator;
        this.prefix   = prefix;
    }

    public Expression getTarget()
    {
        return target;
    }

    public String getOperator()
    {
        return operator;
    }

    public boolean isPrefix()
    {
        return prefix;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitIncrementExpression(this);
    }

    @Override
    public String getLabel()
    {
        return prefix ? operator + "x" : "x" + operator;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return target == null ? List.of() : List.of(target);
    }
}
