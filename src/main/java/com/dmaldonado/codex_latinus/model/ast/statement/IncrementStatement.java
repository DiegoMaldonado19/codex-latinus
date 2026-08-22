package com.dmaldonado.codex_latinus.model.ast.statement;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.List;

/** i++;  i--;  valid anywhere, not only inside a per. */
public class IncrementStatement extends AstNode
{
    private final Expression target;
    private final String     operator;

    public IncrementStatement(Expression target, String operator, int line, int column)
    {
        super(line, column);
        this.target   = target;
        this.operator = operator;
    }

    public Expression getTarget()
    {
        return target;
    }

    public String getOperator()
    {
        return operator;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitIncrementStatement(this);
    }

    @Override
    public String getLabel()
    {
        return operator;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return target == null ? List.of() : List.of(target);
    }
}
