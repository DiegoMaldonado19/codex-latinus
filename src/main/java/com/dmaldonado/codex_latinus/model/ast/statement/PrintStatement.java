package com.dmaldonado.codex_latinus.model.ast.statement;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import java.util.ArrayList;
import java.util.List;

/** &gt;&gt; "Bienvenido" &gt;&gt; comandante; -- translated to %OINK. */
public class PrintStatement extends AstNode
{
    private final List<Expression> values;

    public PrintStatement(List<Expression> values, int line, int column)
    {
        super(line, column);
        this.values = values;
    }

    public List<Expression> getValues()
    {
        return values;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitPrintStatement(this);
    }

    @Override
    public String getLabel()
    {
        return "SALIDA";
    }

    @Override
    public List<AstNode> getChildren()
    {
        return new ArrayList<>(values);
    }
}
