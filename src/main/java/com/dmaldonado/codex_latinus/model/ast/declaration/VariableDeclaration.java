package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/** esto edad : numerus 20; */
public class VariableDeclaration extends AstNode
{
    private final String     name;
    private final String     typeText;
    private final DataType   type;
    private final Expression initialValue;

    public VariableDeclaration(String name, String typeText, Expression initialValue,
                               int line, int column)
    {
        super(line, column);
        this.name         = name;
        this.typeText     = typeText;
        this.type         = DataType.fromText(typeText);
        this.initialValue = initialValue;
    }

    public String getName()
    {
        return name;
    }

    /** Type as written in the source, needed to resolve structure names. */
    public String getTypeText()
    {
        return typeText;
    }

    public DataType getType()
    {
        return type;
    }

    public Expression getInitialValue()
    {
        return initialValue;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitVariableDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "esto " + name + " : " + typeText;
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (initialValue != null)
        {
            children.add(initialValue);
        }
        return children;
    }
}
