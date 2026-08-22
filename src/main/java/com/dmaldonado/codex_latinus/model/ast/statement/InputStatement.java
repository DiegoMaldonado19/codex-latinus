package com.dmaldonado.codex_latinus.model.ast.statement;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/**
 * comandante &lt;&lt;   or   &lt;&lt; comandante   or   &lt;&lt;
 * Translated to %OINK_OINK.
 */
public class InputStatement extends AstNode
{
    private final Expression target;

    public InputStatement(Expression target, int line, int column)
    {
        super(line, column);
        this.target = target;
    }

    /** Null when the read discards the value instead of storing it. */
    public Expression getTarget()
    {
        return target;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitInputStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "ENTRADA";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (target != null)
        {
            children.add(target);
        }
        return children;
    }
}
