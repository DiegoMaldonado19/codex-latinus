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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Traduce a PigLatin RECORRIENDO EL AST, que es lo que el enunciado exige en
 * vez
 * de un replace o una regex sobre el texto fuente. El visitor devuelve String,
 * asi que el resultado se compone de abajo hacia arriba.
 *
 * Nodo por nodo: docs/05-Manual-Tecnico.md (12)
 */
public class PigLatinTranslator implements AstVisitor<String> {
    private static final String INDENT = "    ";

    private int level;
    private boolean translateStrings = false;

    public void setTranslateStrings(boolean translateStrings) {
        this.translateStrings = translateStrings;
    }

    /** Entry point. */
    public String translate(Program program) {
        level = 0;
        return program.accept(this);
    }

    /*
     * =================================================================
     * PROGRAM AND DECLARATIONS
     * =================================================================
     */
    @Override
    public String visitProgram(Program node) {
        StringBuilder out = new StringBuilder();

        if (!node.getGlobals().isEmpty()) {
            out.append(PigLatinWordConverter.convert("VARIABILES")).append(">\n");
            level++;
            for (AstNode global : node.getGlobals()) {
                out.append(global.accept(this)).append('\n');
            }
            level--;
            out.append('\n');
        }

        if (!node.getFunctions().isEmpty()) {
            out.append(PigLatinWordConverter.convert("MUNERA")).append(">\n");
            level++;
            for (FunctionDeclaration function : node.getFunctions()) {
                out.append(function.accept(this)).append('\n');
            }
            level--;
            out.append('\n');
        }

        out.append(PigLatinWordConverter.convert("MAIOR")).append(">\n");
        level++;
        for (AstNode statement : node.getMainStatements()) {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append('\n').append(PigLatinWordConverter.convert("FINIS"))
                .append(";\n").toString();
    }

    /**
     * El valor sintetizado de "esto x : falsus;" se omite: saldria "alsusfay
     * alsusfay".
     */
    @Override
    public String visitVariableDeclaration(VariableDeclaration node) {
        StringBuilder line = new StringBuilder(indent())
                .append(PigLatinWordConverter.convert("esto")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append(" : ").append(typeName(node.getTypeText()));

        if (node.getInitialValue() != null && !isImplicitBoolean(node)) {
            line.append(' ').append(node.getInitialValue().accept(this));
        }
        return line.append(';').toString();
    }

    private boolean isImplicitBoolean(VariableDeclaration node) {
        return node.getInitialValue() instanceof LiteralExpression literal
                && literal.getText().equals(node.getTypeText());
    }

    @Override
    public String visitArrayDeclaration(ArrayDeclaration node) {
        StringBuilder line = new StringBuilder(indent())
                .append(PigLatinWordConverter.convert("series")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append('[').append(node.getSize().accept(this)).append(']')
                .append(" : ").append(typeName(node.getTypeText()));

        if (!node.getInitialValues().isEmpty()) {
            line.append(" {").append(join(node.getInitialValues(), ", ")).append('}');
        }
        return line.append(';').toString();
    }

    @Override
    public String visitStructDeclaration(StructDeclaration node) {
        StringBuilder out = new StringBuilder(indent())
                .append(PigLatinWordConverter.convert("structura")).append(' ')
                .append(PigLatinWordConverter.convert(node.getName()))
                .append(" {\n");

        level++;
        for (StructField field : node.getFields()) {
            out.append(field.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append("} ")
                .append(PigLatinWordConverter.convert("finis")).append(';').toString();
    }

    /** A field declared with "series" is an array, and it carries no size. */
    @Override
    public String visitStructField(StructField node) {
        return indent() + PigLatinWordConverter.convert(node.isArray() ? "series" : "esto")
                + ' ' + PigLatinWordConverter.convert(node.getName())
                + " : " + typeName(node.getTypeText()) + ';';
    }

    @Override
    public String visitParameter(Parameter node) {
        return PigLatinWordConverter.convert("esto")
                + ' ' + PigLatinWordConverter.convert(node.getName())
                + " : " + typeName(node.getTypeText());
    }

    /**
     * No delega en visitBlock: VARIABILES[ ] y las instrucciones van en las mismas
     * llaves.
     */
    @Override
    public String visitFunctionDeclaration(FunctionDeclaration node) {
        String parameters = node.getParameters().stream()
                .map(parameter -> parameter.accept(this))
                .collect(Collectors.joining(", "));

        StringBuilder out = new StringBuilder(indent());

        if (node.returnsValue()) {
            out.append(PigLatinWordConverter.convert("ratio")).append(' ')
                    .append(typeName(node.getReturnTypeText())).append(' ');
        } else {
            out.append(PigLatinWordConverter.convert("actio")).append(' ');
        }

        out.append(PigLatinWordConverter.convert(node.getName()))
                .append('(').append(parameters).append(") {\n");

        level++;
        if (!node.getLocalVariables().isEmpty()) {
            out.append(indent()).append(PigLatinWordConverter.convert("VARIABILES"))
                    .append("[\n");
            level++;
            for (AstNode local : node.getLocalVariables()) {
                out.append(local.accept(this)).append('\n');
            }
            level--;
            out.append(indent()).append("]\n");
        }
        for (AstNode statement : node.getBody().getStatements()) {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append("} ")
                .append(PigLatinWordConverter.convert("finis")).append(';').toString();
    }

    /*
     * =================================================================
     * STATEMENTS
     * =================================================================
     */
    @Override
    public String visitBlock(Block node) {
        StringBuilder out = new StringBuilder("{\n");

        level++;
        for (AstNode statement : node.getStatements()) {
            out.append(statement.accept(this)).append('\n');
        }
        level--;

        return out.append(indent()).append('}').toString();
    }

    @Override
    public String visitAssignment(Assignment node) {
        return indent() + node.getTarget().accept(this) + " = "
                + node.getValue().accept(this) + ';';
    }

    /** Despliega la cadena que AstBuilderVisitor plego en IfStatement anidados. */
    @Override
    public String visitIfStatement(IfStatement node) {
        StringBuilder out = new StringBuilder(indent())
                .append(PigLatinWordConverter.convert("si")).append(" (")
                .append(node.getCondition().accept(this))
                .append(") ").append(node.getThenBranch().accept(this));

        AstNode elseBranch = node.getElseBranch();

        while (elseBranch instanceof IfStatement nested) {
            out.append(' ').append(PigLatinWordConverter.convert("aliter")).append(" (")
                    .append(nested.getCondition().accept(this)).append(") ")
                    .append(nested.getThenBranch().accept(this));
            elseBranch = nested.getElseBranch();
        }

        if (elseBranch instanceof Block block) {
            out.append(' ').append(PigLatinWordConverter.convert("aliter"))
                    .append(' ').append(block.accept(this));
        }
        return out.append(' ').append(PigLatinWordConverter.convert("finis"))
                .append(';').toString();
    }

    @Override
    public String visitWhileStatement(WhileStatement node) {
        return indent() + PigLatinWordConverter.convert("dum")
                + " (" + node.getCondition().accept(this) + ") "
                + node.getBody().accept(this) + ' '
                + PigLatinWordConverter.convert("finis") + ';';
    }

    @Override
    public String visitDoWhileStatement(DoWhileStatement node) {
        return indent() + PigLatinWordConverter.convert("facere")
                + ' ' + node.getBody().accept(this)
                + ' ' + PigLatinWordConverter.convert("dum")
                + " (" + node.getCondition().accept(this) + ");";
    }

    /** La inicializacion ya trae su ';'; la actualizacion es la que lo pierde. */
    @Override
    public String visitForStatement(ForStatement node) {
        String initialization = node.getInitialization().accept(this).stripLeading();
        String update = node.getUpdate().accept(this).stripLeading();

        update = update.endsWith(";") ? update.substring(0, update.length() - 1) : update;

        return indent() + PigLatinWordConverter.convert("per") + " (" + initialization + ' '
                + node.getCondition().accept(this) + "; " + update + ") "
                + node.getBody().accept(this) + ' '
                + PigLatinWordConverter.convert("finis") + ';';
    }

    @Override
    public String visitReturnStatement(ReturnStatement node) {
        String value = node.getValue() == null ? "" : ' ' + node.getValue().accept(this);
        return indent() + PigLatinWordConverter.convert("reddere") + value + ';';
    }

    @Override
    public String visitBreakStatement(BreakStatement node) {
        return indent() + PigLatinWordConverter.convert("interrumpe") + ';';
    }

    @Override
    public String visitContinueStatement(ContinueStatement node) {
        return indent() + PigLatinWordConverter.convert("perge") + ';';
    }

    /** LEY PORCINA: every output arrow of the instruction becomes a %OINK. */
    @Override
    public String visitPrintStatement(PrintStatement node) {
        return indent() + PigLatinWordConverter.OUTPUT_OINK + ' '
                + join(node.getValues(), " " + PigLatinWordConverter.OUTPUT_OINK + " ") + ';';
    }

    /**
     * LEY PORCINA: '<<' becomes %OINK_OINK, in the same position it had in the
     * source, and the read is the one instruction that carries no ';'.
     */
    @Override
    public String visitInputStatement(InputStatement node) {
        String target = node.getTarget() == null ? "" : node.getTarget().accept(this) + ' ';
        return indent() + target + PigLatinWordConverter.INPUT_OINK;
    }

    @Override
    public String visitCallStatement(CallStatement node) {
        return indent() + node.getCall().accept(this) + ';';
    }

    @Override
    public String visitIncrementStatement(IncrementStatement node) {
        return indent() + node.getTarget().accept(this) + node.getOperator() + ';';
    }

    /*
     * =================================================================
     * EXPRESSIONS
     * =================================================================
     */

    @Override
    public String visitBinaryExpression(BinaryExpression node) {
        return operand(node.getLeft()) + ' ' + node.getOperator() + ' '
                + operand(node.getRight());
    }

    @Override
    public String visitUnaryExpression(UnaryExpression node) {
        String operator = "non".equals(node.getOperator())
                ? PigLatinWordConverter.convert("non") + ' '
                : node.getOperator();
        return operator + operand(node.getOperand());
    }

    @Override
    public String visitIncrementExpression(IncrementExpression node) {
        return node.isPrefix()
                ? node.getOperator() + node.getTarget().accept(this)
                : node.getTarget().accept(this) + node.getOperator();
    }

    /** The type says what the literal is, so there is no text sniffing here. */
    @Override
    public String visitLiteralExpression(LiteralExpression node) {
        return switch (node.getType()) {
            case TEXTUM -> translateStrings
                    ? PigLatinWordConverter.convertText(node.getText())
                    : node.getText();
            case BOOLEANO -> PigLatinWordConverter.convert(node.getText());
            default -> node.getText(); // numbers and characters
        };
    }

    @Override
    public String visitIdentifierExpression(IdentifierExpression node) {
        return PigLatinWordConverter.convert(node.getName());
    }

    @Override
    public String visitArrayAccessExpression(ArrayAccessExpression node) {
        return node.getArray().accept(this) + '[' + node.getIndex().accept(this) + ']';
    }

    @Override
    public String visitMemberAccessExpression(MemberAccessExpression node) {
        return node.getOwner().accept(this) + '.'
                + PigLatinWordConverter.convert(node.getMemberName());
    }

    @Override
    public String visitFunctionCallExpression(FunctionCallExpression node) {
        return PigLatinWordConverter.convert(node.getName())
                + '(' + join(node.getArguments(), ", ") + ')';
    }

    /** El nombre del atributo es un identificador mas: tambien se traduce. */
    @Override
    public String visitCompositeLiteralExpression(CompositeLiteralExpression node) {
        if (!node.isNamed()) {
            return '{' + join(node.getValues(), ", ") + '}';
        }

        StringBuilder out = new StringBuilder("{ ");

        for (int i = 0; i < node.getValues().size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(PigLatinWordConverter.convert(node.getFieldNames().get(i)))
                    .append(": ").append(node.getValues().get(i).accept(this));
        }
        return out.append(" }").toString();
    }

    /*
     * =================================================================
     * Support
     * =================================================================
     */

    /** Palabra reservada o nombre de structura: "todo se traduce". */
    private String typeName(String typeText) {
        return typeText == null ? "" : PigLatinWordConverter.convert(typeText);
    }

    /** El AST perdio los parentesis del fuente: se reponen solo entre binarias. */
    private String operand(Expression expression) {
        return expression instanceof BinaryExpression
                ? '(' + expression.accept(this) + ')'
                : expression.accept(this);
    }

    private String join(List<Expression> expressions, String separator) {
        return expressions.stream()
                .map(expression -> expression.accept(this))
                .collect(Collectors.joining(separator));
    }

    private String indent() {
        return INDENT.repeat(Math.max(0, level));
    }
}
