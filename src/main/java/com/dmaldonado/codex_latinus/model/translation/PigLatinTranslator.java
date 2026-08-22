package com.dmaldonado.codex_latinus.model.translation;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.AstVisitor;
import com.dmaldonado.codex_latinus.model.ast.Expression;
import com.dmaldonado.codex_latinus.model.ast.declaration.ArrayDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.FunctionDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.Parameter;
import com.dmaldonado.codex_latinus.model.ast.declaration.Program;
import com.dmaldonado.codex_latinus.model.ast.declaration.StructDeclaration;
import com.dmaldonado.codex_latinus.model.ast.declaration.StructField;
import com.dmaldonado.codex_latinus.model.ast.declaration.VariableDeclaration;
import com.dmaldonado.codex_latinus.model.ast.expression.ArrayAccessExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.BinaryExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.CompositeLiteralExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.FunctionCallExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.IdentifierExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.IncrementExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.LiteralExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.MemberAccessExpression;
import com.dmaldonado.codex_latinus.model.ast.expression.UnaryExpression;
import com.dmaldonado.codex_latinus.model.ast.statement.Assignment;
import com.dmaldonado.codex_latinus.model.ast.statement.Block;
import com.dmaldonado.codex_latinus.model.ast.statement.BreakStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.CallStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ContinueStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.DoWhileStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ForStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.IfStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.IncrementStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.InputStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.PrintStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ReturnStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.WhileStatement;
import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Translates Latin to PigLatin BY WALKING THE AST.
 *
 * The statement forbids replace and regex over the source text: here every node
 * knows how to write itself in PigLatin, and every word (identifier or reserved
 * word) goes through PigLatinWordConverter. It runs on the AST the semantic
 * analyzer already validated, so it never has to check anything.
 *
 * As the visitor returns String, the result is composed bottom-up: the leaves
 * produce text and the inner nodes assemble it with the right indentation.
 */
public class PigLatinTranslator implements AstVisitor<String>
{
    private static final String INDENT = "    ";

    private int     level;
    private boolean translateKeywords = true;
    private boolean translateStrings  = false;

    public void setTranslateKeywords(boolean translateKeywords)
    {
        this.translateKeywords = translateKeywords;
    }

    public void setTranslateStrings(boolean translateStrings)
    {
        this.translateStrings = translateStrings;
    }

    /** Entry point. */
    public String translate(Program program)
    {
        level = 0;
        return program.accept(this);
    }

    /* =================================================================
     * PROGRAM AND DECLARATIONS
     * ================================================================= */
    @Override
    public String visitProgram(Program node)
    {
        StringBuilder out = new StringBuilder();

        if (!node.getGlobals().isEmpty())
        {
            out.append(keyword("VARIABILES")).append(">\n");
            level++;
            for (AstNode global : node.getGlobals())
            {
                out.append(global.accept(this)).append('\n');
            }
            level--;
            out.append('\n');
        }

        if (!node.getFunctions().isEmpty())
        {
            out.append(keyword("MUNERA")).append(">\n");
            level++;
            for (FunctionDeclaration function : node.getFunctions())
            {
                out.append(function.accept(this)).append('\n');
            }
            level--;
            out.append('\n');
        }

        out.append(keyword("MAIOR")).append(">\n");
        level++;
        for (AstNode statement : node.getMainStatements())
        {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append('\n').append(keyword("FINIS")).append(";\n").toString();
    }

    /**
     * "esto bloqueado : falsus;" declares the value with the same word it uses
     * as the type, and AstBuilderVisitor materializes that value in the AST.
     * Writing it back would produce "alsusfay alsusfay", so that synthesized
     * literal is dropped here and the declaration keeps its original shape.
     */
    @Override
    public String visitVariableDeclaration(VariableDeclaration node)
    {
        StringBuilder line = new StringBuilder(indent())
                .append(keyword("esto")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append(" : ").append(typeName(node.getTypeText()));

        if (node.getInitialValue() != null && !isImplicitBoolean(node))
        {
            line.append(' ').append(node.getInitialValue().accept(this));
        }
        return line.append(';').toString();
    }

    private boolean isImplicitBoolean(VariableDeclaration node)
    {
        return node.getInitialValue() instanceof LiteralExpression literal
                && literal.getText().equals(node.getTypeText());
    }

    @Override
    public String visitArrayDeclaration(ArrayDeclaration node)
    {
        StringBuilder line = new StringBuilder(indent())
                .append(keyword("series")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append('[').append(node.getSize().accept(this)).append(']')
                .append(" : ").append(typeName(node.getTypeText()));

        if (!node.getInitialValues().isEmpty())
        {
            line.append(" {").append(join(node.getInitialValues(), ", ")).append('}');
        }
        return line.append(';').toString();
    }

    @Override
    public String visitStructDeclaration(StructDeclaration node)
    {
        StringBuilder out = new StringBuilder(indent())
                .append(keyword("structura")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append(" {\n");

        level++;
        for (StructField field : node.getFields())
        {
            out.append(field.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append("} ").append(keyword("finis")).append(';').toString();
    }

    /** A field declared with "series" is an array, and it carries no size. */
    @Override
    public String visitStructField(StructField node)
    {
        return indent() + keyword(node.isArray() ? "series" : "esto") + ' '
             + PigLatinWordConverter.convert(node.getName())
             + " : " + typeName(node.getTypeText()) + ';';
    }

    @Override
    public String visitParameter(Parameter node)
    {
        return keyword("esto") + ' ' + PigLatinWordConverter.convert(node.getName())
             + " : " + typeName(node.getTypeText());
    }

    /**
     * The body is assembled here instead of delegating to visitBlock, because
     * the AST keeps the VARIABILES[ ] section apart from the statements and
     * both have to be written back inside the same braces.
     */
    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node)
    {
        String parameters = node.getParameters().stream()
                .map(parameter -> parameter.accept(this))
                .collect(Collectors.joining(", "));

        StringBuilder out = new StringBuilder(indent());

        if (node.returnsValue())
        {
            out.append(keyword("ratio")).append(' ')
               .append(typeName(node.getReturnTypeText())).append(' ');
        }
        else
        {
            out.append(keyword("actio")).append(' ');
        }

        out.append(PigLatinWordConverter.convert(node.getName()))
           .append('(').append(parameters).append(") {\n");

        level++;
        if (!node.getLocalVariables().isEmpty())
        {
            out.append(indent()).append(keyword("VARIABILES")).append("[\n");
            level++;
            for (AstNode local : node.getLocalVariables())
            {
                out.append(local.accept(this)).append('\n');
            }
            level--;
            out.append(indent()).append("]\n");
        }
        for (AstNode statement : node.getBody().getStatements())
        {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append("} ").append(keyword("finis")).append(';').toString();
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */
    @Override
    public String visitBlock(Block node)
    {
        StringBuilder out = new StringBuilder("{\n");

        level++;
        for (AstNode statement : node.getStatements())
        {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append('}').toString();
    }

    @Override
    public String visitAssignment(Assignment node)
    {
        return indent() + node.getTarget().accept(this) + " = "
             + node.getValue().accept(this) + ';';
    }

    /**
     * AstBuilderVisitor folds "aliter (c)" into a nested IfStatement, so the
     * chain is unrolled back here: otherwise the output would nest a whole new
     * conditional block for every link of the chain.
     */
    @Override
    public String visitIfStatement(IfStatement node)
    {
        StringBuilder out = new StringBuilder(indent())
                .append(keyword("si")).append(" (").append(node.getCondition().accept(this))
                .append(") ").append(node.getThenBranch().accept(this));

        AstNode elseBranch = node.getElseBranch();

        while (elseBranch instanceof IfStatement nested)
        {
            out.append(' ').append(keyword("aliter")).append(" (")
               .append(nested.getCondition().accept(this)).append(") ")
               .append(nested.getThenBranch().accept(this));
            elseBranch = nested.getElseBranch();
        }

        if (elseBranch instanceof Block block)
        {
            out.append(' ').append(keyword("aliter")).append(' ').append(block.accept(this));
        }
        return out.append(' ').append(keyword("finis")).append(';').toString();
    }

    @Override
    public String visitWhileStatement(WhileStatement node)
    {
        return indent() + keyword("dum") + " (" + node.getCondition().accept(this) + ") "
             + node.getBody().accept(this) + ' ' + keyword("finis") + ';';
    }

    @Override
    public String visitDoWhileStatement(DoWhileStatement node)
    {
        return indent() + keyword("facere") + ' ' + node.getBody().accept(this)
             + ' ' + keyword("dum") + " (" + node.getCondition().accept(this) + ");";
    }

    /**
     * The initialization is a statement and already carries its own ';', which
     * is exactly what the "per (init; cond; update)" syntax needs. The update
     * is a statement too, so its ';' is the one that has to go.
     */
    @Override
    public String visitForStatement(ForStatement node)
    {
        String initialization = node.getInitialization().accept(this).stripLeading();
        String update         = node.getUpdate().accept(this).stripLeading();

        update = update.endsWith(";") ? update.substring(0, update.length() - 1) : update;

        return indent() + keyword("per") + " (" + initialization + ' '
             + node.getCondition().accept(this) + "; " + update + ") "
             + node.getBody().accept(this) + ' ' + keyword("finis") + ';';
    }

    @Override
    public String visitReturnStatement(ReturnStatement node)
    {
        String value = node.getValue() == null ? "" : ' ' + node.getValue().accept(this);
        return indent() + keyword("reddere") + value + ';';
    }

    @Override
    public String visitBreakStatement(BreakStatement node)
    {
        return indent() + keyword("interrumpe") + ';';
    }

    @Override
    public String visitContinueStatement(ContinueStatement node)
    {
        return indent() + keyword("perge") + ';';
    }

    /** LEY PORCINA: every output arrow of the instruction becomes a %OINK. */
    @Override
    public String visitPrintStatement(PrintStatement node)
    {
        return indent() + PigLatinWordConverter.OUTPUT_OINK + ' '
             + join(node.getValues(), " " + PigLatinWordConverter.OUTPUT_OINK + " ") + ';';
    }

    /**
     * LEY PORCINA: the read arrow becomes %OINK_OINK. The prefix form is used
     * because a bare read with no target is valid, and the target can be a
     * whole chain such as mi_selva.animales[1].
     */
    @Override
    public String visitInputStatement(InputStatement node)
    {
        String target = node.getTarget() == null ? "" : ' ' + node.getTarget().accept(this);
        return indent() + PigLatinWordConverter.INPUT_OINK + target + ';';
    }

    @Override
    public String visitCallStatement(CallStatement node)
    {
        return indent() + node.getCall().accept(this) + ';';
    }

    @Override
    public String visitIncrementStatement(IncrementStatement node)
    {
        return indent() + node.getTarget().accept(this) + node.getOperator() + ';';
    }

    /* =================================================================
     * EXPRESSIONS
     * ================================================================= */

    @Override
    public String visitBinaryExpression(BinaryExpression node)
    {
        return operand(node.getLeft()) + ' ' + node.getOperator() + ' '
             + operand(node.getRight());
    }

    @Override
    public String visitUnaryExpression(UnaryExpression node)
    {
        String operator = "non".equals(node.getOperator())
                ? keyword("non") + ' '
                : node.getOperator();
        return operator + operand(node.getOperand());
    }

    @Override
    public String visitIncrementExpression(IncrementExpression node)
    {
        return node.isPrefix()
                ? node.getOperator() + node.getTarget().accept(this)
                : node.getTarget().accept(this) + node.getOperator();
    }

    /** The type says what the literal is, so there is no text sniffing here. */
    @Override
    public String visitLiteralExpression(LiteralExpression node)
    {
        return switch (node.getType())
        {
            case TEXTUM   -> translateStrings
                             ? PigLatinWordConverter.convertText(node.getText())
                             : node.getText();
            case BOOLEANO -> keyword(node.getText());
            default       -> node.getText();      // numbers and characters
        };
    }

    @Override
    public String visitIdentifierExpression(IdentifierExpression node)
    {
        return PigLatinWordConverter.convert(node.getName());
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node)
    {
        return node.getArray().accept(this) + '[' + node.getIndex().accept(this) + ']';
    }

    @Override
    public String visitMemberAccessExpression(MemberAccessExpression node)
    {
        return node.getOwner().accept(this) + '.'
             + PigLatinWordConverter.convert(node.getMemberName());
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node)
    {
        return PigLatinWordConverter.convert(node.getName())
             + '(' + join(node.getArguments(), ", ") + ')';
    }

    /**
     * The attribute name of a named literal is an identifier like any other, so
     * it is translated too: the statement asks for the code to be converted
     * "palabra por palabra", not only the variable names.
     */
    @Override
    public String visitCompositeLiteralExpression(CompositeLiteralExpression node)
    {
        if (!node.isNamed())
        {
            return '{' + join(node.getValues(), ", ") + '}';
        }

        StringBuilder out = new StringBuilder("{ ");

        for (int i = 0; i < node.getValues().size(); i++)
        {
            if (i > 0)
            {
                out.append(", ");
            }
            out.append(PigLatinWordConverter.convert(node.getFieldNames().get(i)))
               .append(": ").append(node.getValues().get(i).accept(this));
        }
        return out.append(" }").toString();
    }

    /* =================================================================
     * Support
     * ================================================================= */

    /** Reserved words follow the checkbox; identifiers are always translated. */
    private String keyword(String reservedWord)
    {
        return translateKeywords ? PigLatinWordConverter.convert(reservedWord) : reservedWord;
    }

    /**
     * A type is either a reserved word of the language or the name of a
     * structura. The name of a structura is an identifier, so it follows the
     * same rule as the declaration that created it and is always translated.
     */
    private String typeName(String typeText)
    {
        if (typeText == null)
        {
            return "";
        }
        return DataType.fromText(typeText) == DataType.ESTRUCTURA
                ? PigLatinWordConverter.convert(typeText)
                : keyword(typeText);
    }

    /**
     * The AST folds the precedence chain and drops the grouping the source had,
     * so a binary expression used as the operand of another one is written back
     * inside parentheses. Anywhere else the syntax already delimits the whole
     * expression -- si (...), an index, an argument -- and they would be noise.
     */
    private String operand(Expression expression)
    {
        return expression instanceof BinaryExpression
                ? '(' + expression.accept(this) + ')'
                : expression.accept(this);
    }

    private String join(List<Expression> expressions, String separator)
    {
        return expressions.stream()
                .map(expression -> expression.accept(this))
                .collect(Collectors.joining(separator));
    }

    private String indent()
    {
        return INDENT.repeat(Math.max(0, level));
    }
}
