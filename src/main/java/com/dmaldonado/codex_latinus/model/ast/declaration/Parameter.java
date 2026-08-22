package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.List;

/** esto fuerza : numerus, inside a function signature. */
public class Parameter extends AstNode
{
    private final String   name;
    private final String   typeText;
    private final DataType type;

    public Parameter(String name, String typeText, int line, int column)
    {
        super(line, column);
        this.name     = name;
        this.typeText = typeText;
        this.type     = DataType.fromText(typeText);
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

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitParameter(this);
    }

    @Override
    public String getLabel()
    {
        return "param " + name + " : " + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        return List.of();
    }
}
