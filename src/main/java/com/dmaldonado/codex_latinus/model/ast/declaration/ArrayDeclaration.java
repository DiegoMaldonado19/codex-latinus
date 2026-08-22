package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/** series mis_enteros[2] : numerus {1, 1}; */
public class ArrayDeclaration extends AstNode
{
    private final String           name;
    private final Expression       size;
    private final String           typeText;
    private final DataType         elementType;
    private final List<Expression> initialValues;

    public ArrayDeclaration(String name, Expression size, String typeText,
                            List<Expression> initialValues, int line, int column)
    {
        super(line, column);
        this.name          = name;
        this.size          = size;
        this.typeText      = typeText;
        this.elementType   = DataType.fromText(typeText);
        this.initialValues = initialValues;
    }

    public String getName()
    {
        return name;
    }

    public Expression getSize()
    {
        return size;
    }

    public String getTypeText()
    {
        return typeText;
    }

    public DataType getElementType()
    {
        return elementType;
    }

    /** Empty when the array was declared without initial values. */
    public List<Expression> getInitialValues()
    {
        return initialValues;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitArrayDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return "series " + name + " : " + (typeText == null ? "?" : typeText);
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>();
        if (size != null)
        {
            children.add(size);
        }
        children.addAll(initialValues);
        return children;
    }
}
