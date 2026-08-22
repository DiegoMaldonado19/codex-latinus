package com.dmaldonado.codex_latinus.model.ast.expression;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.List;

/**
 * 10, 9.81, "texto", 'a', verum, falsus.
 *
 * The type comes straight from the grammar alternative that matched, so the
 * semantic analyzer never has to guess it back from the text.
 */
public class LiteralExpression extends Expression
{
    private final String   text;
    private final DataType type;

    public LiteralExpression(String text, DataType type, int line, int column)
    {
        super(line, column);
        this.text = text;
        this.type = type;
    }

    /** Raw text, quotes included for textum and littera. */
    public String getText()
    {
        return text;
    }

    public DataType getType()
    {
        return type;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitLiteralExpression(this);
    }

    @Override
    public String getLabel()
    {
        return text;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
