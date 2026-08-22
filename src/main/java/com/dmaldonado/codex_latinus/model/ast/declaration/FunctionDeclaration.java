package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.statement.Block;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.ArrayList;
import java.util.List;

/**
 * actio atacarCerdos(...) { ... } finis;  and
 * ratio numerus calcularPoder(...) { ... } finis;
 *
 * localVariables holds the declarations written in the VARIABILES[ ] section,
 * kept apart from the body so the semantic analyzer can tell a variable
 * declared where the language allows it from one declared mid function.
 */
public class FunctionDeclaration extends AstNode
{
    private final String          name;
    private final String          returnTypeText;
    private final DataType        returnType;
    private final List<Parameter> parameters;
    private final List<AstNode>   localVariables;
    private final Block           body;
    private final boolean         returnsValue;

    public FunctionDeclaration(String name, String returnTypeText, List<Parameter> parameters,
                               List<AstNode> localVariables, Block body, boolean returnsValue,
                               int line, int column)
    {
        super(line, column);
        this.name           = name;
        this.returnTypeText = returnTypeText;
        this.returnType     = returnsValue ? DataType.fromText(returnTypeText) : DataType.VOID;
        this.parameters     = parameters;
        this.localVariables = localVariables;
        this.body           = body;
        this.returnsValue   = returnsValue;
    }

    public String getName()
    {
        return name;
    }

    public String getReturnTypeText()
    {
        return returnTypeText;
    }

    public DataType getReturnType()
    {
        return returnType;
    }

    public List<Parameter> getParameters()
    {
        return parameters;
    }

    /** Declarations of the VARIABILES[ ] section; empty when there is none. */
    public List<AstNode> getLocalVariables()
    {
        return localVariables;
    }

    public Block getBody()
    {
        return body;
    }

    public boolean returnsValue()
    {
        return returnsValue;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitFunctionDeclaration(this);
    }

    @Override
    public String getLabel()
    {
        return (returnsValue ? "ratio " + returnTypeText + " " : "actio ") + name;
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(parameters);
        children.addAll(localVariables);
        if (body != null)
        {
            children.add(body);
        }
        return children;
    }
}
