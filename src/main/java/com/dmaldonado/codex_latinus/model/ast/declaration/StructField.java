package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.List;

/**
 * One attribute inside a structura.
 *
 * An array field carries no size here (series animales : Animal;) because the
 * dimension is only given when the variable is declared.
 */
public class StructField extends AstNode
{
    private final String   name;
    private final String   typeText;
    private final DataType type;
    private final boolean  array;

    public StructField(String name, String typeText, boolean array, int line, int column)
    {
        super(line, column);
        this.name     = name;
        this.typeText = typeText;
        this.type     = DataType.fromText(typeText);
        this.array    = array;
    }

    public String getName()
    {
        return name;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public DataType getType()
    {
        return type;
    }

    public boolean isArray()
    {
        return array;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitStructField(this);
    }

    @Override
    public String getLabel()
    {
        return (array ? "series " : "esto ") + name + " : " + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
