package com.dmaldonado.codex_latinus.model.ast.declaration;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import java.util.ArrayList;
import java.util.List;

/** Root of the AST: the three sections VARIABILES, MUNERA and MAIOR. */
public class Program extends AstNode
{
    private final List<AstNode>             globals;
    private final List<FunctionDeclaration> functions;
    private final List<AstNode>             mainStatements;

    public Program(List<AstNode> globals, List<FunctionDeclaration> functions,
                   List<AstNode> mainStatements, int line, int column)
    {
        super(line, column);
        this.globals        = globals;
        this.functions      = functions;
        this.mainStatements = mainStatements;
    }

    public List<AstNode> getGlobals()
    {
        return globals;
    }

    public List<FunctionDeclaration> getFunctions()
    {
        return functions;
    }

    public List<AstNode> getMainStatements()
    {
        return mainStatements;
    }

    @Override
    public <T> T accept(AstVisitor<T> visitor)
    {
        return visitor.visitProgram(this);
    }

    @Override
    public String getLabel()
    {
        return "PROGRAMA";
    }

    @Override
    public List<AstNode> getChildren()
    {
        List<AstNode> children = new ArrayList<>(globals);
        children.addAll(functions);
        children.addAll(mainStatements);
        return children;
    }
}
