package com.dmaldonado.codex_latinus.model.analysis;

import com.dmaldonado.codex_latinus.grammar.LatinParser;
import com.dmaldonado.codex_latinus.grammar.LatinParserBaseVisitor;
import com.dmaldonado.codex_latinus.model.ast.AstNode;
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
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Builds the compiler's own AST from the parse tree, using the visitor that
 * ANTLR generates.
 *
 * How it works: visitXxx(ctx) returns the AST node for that rule. To get the
 * node of a child, the method calls visit(child) and uses the value it returns.
 * The recursion is explicit, and the return value is the only channel between a
 * rule and its parent, so no side table is needed:
 *
 *   visitPrimariaEntero   returns LiteralExpression   (leaves)
 *   visitExpresionAditiva returns BinaryExpression
 *   visitPrograma         returns Program             (root)
 *
 * The grammar labels every alternative (# primariaEntero, # entradaSufija,
 * # literalConNombre ...), so ANTLR generates one visit method per alternative
 * and no if/instanceof is needed to tell them apart.
 *
 * Rules with a SINGLE child are not overridden here on purpose: the inherited
 * visitChildren() already returns that child's result, which is exactly what a
 * bridge rule needs. That covers instruccion, expresion, declaracionGlobal,
 * declaracionFuncion, inicializacionPara, primariaLlamada, primariaLiteral and
 * unariaSufijoDelegado. Rules with several children, such as
 * primariaAgrupacion, DO need an override, because the default would return the
 * result of the last child, which is a token.
 */
public class AstBuilderVisitor extends LatinParserBaseVisitor<AstNode>
{
    /** Entry point: walks the parse tree and returns the root of the AST. */
    public Program build(LatinParser.ProgramaContext parseTree)
    {
        return visit(parseTree) instanceof Program program ? program : null;
    }

    /* =================================================================
     * PROGRAM
     * ================================================================= */
    @Override
    public AstNode visitPrograma(LatinParser.ProgramaContext ctx)
    {
        List<AstNode> globals             = new ArrayList<>();
        List<FunctionDeclaration> methods = new ArrayList<>();
        List<AstNode> mainStatements      = new ArrayList<>();

        if (ctx.seccionVariables() != null)
        {
            for (LatinParser.DeclaracionGlobalContext global : ctx.seccionVariables().declaracionGlobal())
            {
                add(globals, visit(global));
            }
        }
        if (ctx.seccionFunciones() != null)
        {
            for (LatinParser.DeclaracionFuncionContext function : ctx.seccionFunciones().declaracionFuncion())
            {
                if (visit(function) instanceof FunctionDeclaration declaration)
                {
                    methods.add(declaration);
                }
            }
        }
        if (ctx.seccionPrincipal() != null)
        {
            for (LatinParser.InstruccionContext statement : ctx.seccionPrincipal().instruccion())
            {
                add(mainStatements, visit(statement));
            }
        }
        return new Program(globals, methods, mainStatements, line(ctx), column(ctx));
    }

    /* =================================================================
     * DECLARATIONS
     * ================================================================= */
    @Override
    public AstNode visitDeclaracionVariableSimple(LatinParser.DeclaracionVariableSimpleContext ctx)
    {
        Expression value = expression(ctx.expresion());

        if (value == null)
        {
            value = implicitBooleanValue(ctx.tipo());
        }
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(), value,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionVariableEstructura(LatinParser.DeclaracionVariableEstructuraContext ctx)
    {
        return new VariableDeclaration(ctx.ID().getText(), ctx.tipo().getText(),
                expression(ctx.literalCompuesto()), line(ctx), column(ctx));
    }

    /**
     * "esto activo : verum;" declares a boolean whose initial value is the same
     * word used as the type. It is materialized here so the rest of the
     * compiler sees an ordinary initialization.
     */
    private Expression implicitBooleanValue(LatinParser.TipoContext ctx)
    {
        String text = ctx.getText();

        if ("verum".equals(text) || "falsus".equals(text))
        {
            return new LiteralExpression(text, DataType.BOOLEANO, line(ctx), column(ctx));
        }
        return null;
    }

    @Override
    public AstNode visitDeclaracionArreglo(LatinParser.DeclaracionArregloContext ctx)
    {
        List<Expression> values = expressionList(ctx.listaExpresiones());

        return new ArrayDeclaration(ctx.ID().getText(), expression(ctx.expresion()),
                arrayTypeText(ctx, values), values, line(ctx), column(ctx));
    }

    /**
     * "series valores[2] : {verum, verum};" omits the type: it is the form the
     * assistant gave before 'bool' existed (Telegram 6/08 22:08). The element
     * type is then inferred from the first literal value. With no type and no
     * values there is nothing to infer, and the semantic analyzer reports it.
     */
    private String arrayTypeText(LatinParser.DeclaracionArregloContext ctx, List<Expression> values)
    {
        if (ctx.tipo() != null)
        {
            return ctx.tipo().getText();
        }
        if (!values.isEmpty() && values.get(0) instanceof LiteralExpression literal)
        {
            return literal.getType().getLatinName();
        }
        return null;
    }

    @Override
    public AstNode visitCampoSimple(LatinParser.CampoSimpleContext ctx)
    {
        return new StructField(ctx.ID().getText(), ctx.tipo().getText(), false,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitCampoArreglo(LatinParser.CampoArregloContext ctx)
    {
        return new StructField(ctx.ID().getText(), ctx.tipo().getText(), true,
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitDeclaracionEstructura(LatinParser.DeclaracionEstructuraContext ctx)
    {
        List<StructField> fields = new ArrayList<>();

        for (LatinParser.AtributoEstructuraContext attribute : ctx.atributoEstructura())
        {
            if (visit(attribute) instanceof StructField field)
            {
                fields.add(field);
            }
        }
        return new StructDeclaration(ctx.ID().getText(), fields, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitParametro(LatinParser.ParametroContext ctx)
    {
        return new Parameter(ctx.ID().getText(), ctx.tipo().getText(), line(ctx), column(ctx));
    }

    /* =================================================================
     * FUNCTIONS
     * ================================================================= */
    @Override
    public AstNode visitFuncionSinRetorno(LatinParser.FuncionSinRetornoContext ctx)
    {
        return new FunctionDeclaration(ctx.ID().getText(), "void",
                parameters(ctx.listaParametros()), localVariables(ctx.cuerpoFuncion()),
                block(ctx.cuerpoFuncion()), false, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitFuncionConRetorno(LatinParser.FuncionConRetornoContext ctx)
    {
        return new FunctionDeclaration(ctx.ID().getText(), ctx.tipo().getText(),
                parameters(ctx.listaParametros()), localVariables(ctx.cuerpoFuncion()),
                block(ctx.cuerpoFuncion()), true, line(ctx), column(ctx));
    }

    /**
     * The body keeps only the statements. The declarations of the VARIABILES[ ]
     * section are pulled out separately by localVariables(), so the semantic
     * analyzer can tell a variable declared where the language allows it from
     * one declared in the middle of the function.
     */
    @Override
    public AstNode visitCuerpoFuncion(LatinParser.CuerpoFuncionContext ctx)
    {
        return new Block(statements(ctx.instruccion()), line(ctx), column(ctx));
    }

    private List<AstNode> localVariables(LatinParser.CuerpoFuncionContext ctx)
    {
        List<AstNode> declarations = new ArrayList<>();

        if (ctx != null && ctx.seccionVariablesLocales() != null)
        {
            for (LatinParser.DeclaracionGlobalContext declaration : ctx.seccionVariablesLocales().declaracionGlobal())
            {
                add(declarations, visit(declaration));
            }
        }
        return declarations;
    }

    @Override
    public AstNode visitBloque(LatinParser.BloqueContext ctx)
    {
        return new Block(statements(ctx.instruccion()), line(ctx), column(ctx));
    }

    /* =================================================================
     * STATEMENTS
     * ================================================================= */
    @Override
    public AstNode visitAsignacionSimple(LatinParser.AsignacionSimpleContext ctx)
    {
        return new Assignment(expression(ctx.destino()), expression(ctx.expresion()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitAsignacionEstructura(LatinParser.AsignacionEstructuraContext ctx)
    {
        return new Assignment(expression(ctx.destino()), expression(ctx.literalCompuesto()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionIncremento(LatinParser.InstruccionIncrementoContext ctx)
    {
        return new IncrementStatement(expression(ctx.destino()),
                ctx.INCREMENTO() != null ? "++" : "--", line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionLlamada(LatinParser.InstruccionLlamadaContext ctx)
    {
        return visit(ctx.llamadaFuncion()) instanceof FunctionCallExpression call
                ? new CallStatement(call, line(ctx), column(ctx))
                : null;
    }

    /**
     * Folds the aliter chain from right to left, so
     * "si (a) A aliter (b) B aliter C" becomes If(a, A, If(b, B, C)).
     * The nested IfStatement is what the translator recognizes to print
     * "aliter (b)" instead of "aliter { si (b)".
     */
    @Override
    public AstNode visitInstruccionSi(LatinParser.InstruccionSiContext ctx)
    {
        // bloque(0) is the si branch; bloque(1) exists only when a plain
        // aliter closes the chain. The conditional ones live in their own rule.
        AstNode elseBranch = ctx.ALITER() != null ? block(ctx.bloque(1)) : null;

        List<LatinParser.AliterCondicionalContext> chain = ctx.aliterCondicional();

        for (int i = chain.size() - 1; i >= 0; i--)
        {
            LatinParser.AliterCondicionalContext link = chain.get(i);
            elseBranch = new IfStatement(expression(link.expresion()), block(link.bloque()),
                    elseBranch, line(link), column(link));
        }

        return new IfStatement(expression(ctx.expresion()), block(ctx.bloque(0)),
                elseBranch, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionMientras(LatinParser.InstruccionMientrasContext ctx)
    {
        return new WhileStatement(expression(ctx.expresion()), block(ctx.bloque()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionHacerMientras(LatinParser.InstruccionHacerMientrasContext ctx)
    {
        return new DoWhileStatement(block(ctx.bloque()), expression(ctx.expresion()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitActualizacionUnaria(LatinParser.ActualizacionUnariaContext ctx)
    {
        return new IncrementStatement(expression(ctx.destino()),
                ctx.INCREMENTO() != null ? "++" : "--", line(ctx), column(ctx));
    }

    @Override
    public AstNode visitActualizacionAsignacion(LatinParser.ActualizacionAsignacionContext ctx)
    {
        return new Assignment(expression(ctx.destino()), expression(ctx.expresion()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionPara(LatinParser.InstruccionParaContext ctx)
    {
        return new ForStatement(child(ctx.inicializacionPara()), expression(ctx.expresion()),
                child(ctx.actualizacionPara()), block(ctx.bloque()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionRetorno(LatinParser.InstruccionRetornoContext ctx)
    {
        return new ReturnStatement(expression(ctx.expresion()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionInterrumpe(LatinParser.InstruccionInterrumpeContext ctx)
    {
        return new BreakStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionPerge(LatinParser.InstruccionPergeContext ctx)
    {
        return new ContinueStatement(line(ctx), column(ctx));
    }

    @Override
    public AstNode visitInstruccionSalida(LatinParser.InstruccionSalidaContext ctx)
    {
        List<Expression> values = new ArrayList<>();

        for (LatinParser.ExpresionContext value : ctx.expresion())
        {
            Expression printed = expression(value);
            if (printed != null)
            {
                values.add(printed);
            }
        }
        return new PrintStatement(values, line(ctx), column(ctx));
    }

    // Both forms of the read produce the same node: only the position of the
    // target changes in the source, not its meaning.
    @Override
    public AstNode visitEntradaSufija(LatinParser.EntradaSufijaContext ctx)
    {
        return new InputStatement(expression(ctx.destino()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitEntradaPrefija(LatinParser.EntradaPrefijaContext ctx)
    {
        return new InputStatement(expression(ctx.destino()), line(ctx), column(ctx));
    }

    /* =================================================================
     * ASSIGNMENT TARGETS
     * ================================================================= */
    @Override
    public AstNode visitDestino(LatinParser.DestinoContext ctx)
    {
        Expression current = new IdentifierExpression(ctx.ID().getText(), line(ctx), column(ctx));

        for (LatinParser.SufijoDestinoContext suffix : ctx.sufijoDestino())
        {
            if (suffix instanceof LatinParser.AccesoIndiceContext index)
            {
                current = new ArrayAccessExpression(current, expression(index.expresion()),
                        line(index), column(index));
            }
            else
            {
                LatinParser.AccesoAtributoContext member = (LatinParser.AccesoAtributoContext) suffix;
                current = new MemberAccessExpression(current, member.ID().getText(),
                        line(member), column(member));
            }
        }
        return current;
    }

    /* =================================================================
     * EXPRESSIONS
     *
     * The six precedence rules all share the shape  A : B (op B)* , so a
     * single method folds them, LEFT associative:  a - b - c => ((a - b) - c)
     * ================================================================= */
    @Override public AstNode visitExpresionOr(LatinParser.ExpresionOrContext ctx)                         { return fold(ctx); }
    @Override public AstNode visitExpresionAnd(LatinParser.ExpresionAndContext ctx)                       { return fold(ctx); }
    @Override public AstNode visitExpresionIgualdad(LatinParser.ExpresionIgualdadContext ctx)             { return fold(ctx); }
    @Override public AstNode visitExpresionRelacional(LatinParser.ExpresionRelacionalContext ctx)         { return fold(ctx); }
    @Override public AstNode visitExpresionAditiva(LatinParser.ExpresionAditivaContext ctx)               { return fold(ctx); }
    @Override public AstNode visitExpresionMultiplicativa(LatinParser.ExpresionMultiplicativaContext ctx) { return fold(ctx); }

    /**
     * With a single operand the loop never runs and the child's node passes
     * through untouched, so the AST does not fill up with one child binary
     * nodes.
     */
    private AstNode fold(ParserRuleContext ctx)
    {
        Expression left = expression(ctx.getChild(0));

        for (int i = 1; i + 1 < ctx.getChildCount(); i += 2)
        {
            String operator  = ctx.getChild(i).getText();
            Expression right = expression(ctx.getChild(i + 1));
            left = new BinaryExpression(left, operator, right, line(ctx), column(ctx));
        }
        return left;
    }

    @Override
    public AstNode visitUnariaNegacionLogica(LatinParser.UnariaNegacionLogicaContext ctx)
    {
        return new UnaryExpression("non", expression(ctx.expresionUnaria()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaNegativo(LatinParser.UnariaNegativoContext ctx)
    {
        return new UnaryExpression("-", expression(ctx.expresionUnaria()), line(ctx), column(ctx));
    }

    @Override
    public AstNode visitUnariaPrefija(LatinParser.UnariaPrefijaContext ctx)
    {
        return new IncrementExpression(expression(ctx.expresionUnaria()),
                ctx.INCREMENTO() != null ? "++" : "--", true, line(ctx), column(ctx));
    }

    /** Each suffix wraps the previous one, so arr[i].prop[j] chains correctly. */
    @Override
    public AstNode visitExpresionSufijo(LatinParser.ExpresionSufijoContext ctx)
    {
        Expression current = expression(ctx.expresionPrimaria());

        for (LatinParser.SufijoExpresionContext suffix : ctx.sufijoExpresion())
        {
            if (suffix instanceof LatinParser.SufijoIndiceContext index)
            {
                current = new ArrayAccessExpression(current, expression(index.expresion()),
                        line(index), column(index));
            }
            else if (suffix instanceof LatinParser.SufijoAtributoContext member)
            {
                current = new MemberAccessExpression(current, member.ID().getText(),
                        line(member), column(member));
            }
            else
            {
                String operator = suffix instanceof LatinParser.SufijoIncrementoContext ? "++" : "--";
                current = new IncrementExpression(current, operator, false,
                        line(suffix), column(suffix));
            }
        }
        return current;
    }

    /* -------- Primary expressions (leaves of the AST) -------- */
    @Override
    public AstNode visitPrimariaEntero(LatinParser.PrimariaEnteroContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.NUMERUS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaDecimal(LatinParser.PrimariaDecimalContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.DECIMALIS, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaTexto(LatinParser.PrimariaTextoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.TEXTUM, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaCaracter(LatinParser.PrimariaCaracterContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.LITTERA, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaVerdadero(LatinParser.PrimariaVerdaderoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaFalso(LatinParser.PrimariaFalsoContext ctx)
    {
        return new LiteralExpression(ctx.getText(), DataType.BOOLEANO, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitPrimariaIdentificador(LatinParser.PrimariaIdentificadorContext ctx)
    {
        return new IdentifierExpression(ctx.getText(), line(ctx), column(ctx));
    }

    /** Parentheses add no node: the AST already carries the grouping. */
    @Override
    public AstNode visitPrimariaAgrupacion(LatinParser.PrimariaAgrupacionContext ctx)
    {
        return visit(ctx.expresion());
    }

    @Override
    public AstNode visitLiteralConNombre(LatinParser.LiteralConNombreContext ctx)
    {
        List<String> fieldNames = new ArrayList<>();
        List<Expression> values = new ArrayList<>();

        for (LatinParser.CampoLiteralContext field : ctx.campoLiteral())
        {
            fieldNames.add(field.ID().getText());
            values.add(expression(field.expresion()));
        }
        return new CompositeLiteralExpression(fieldNames, values, line(ctx), column(ctx));
    }

    @Override
    public AstNode visitLiteralPosicional(LatinParser.LiteralPosicionalContext ctx)
    {
        return new CompositeLiteralExpression(null, expressionList(ctx.listaExpresiones()),
                line(ctx), column(ctx));
    }

    @Override
    public AstNode visitLlamadaFuncion(LatinParser.LlamadaFuncionContext ctx)
    {
        return new FunctionCallExpression(ctx.ID().getText(),
                expressionList(ctx.listaExpresiones()), line(ctx), column(ctx));
    }

    /* =================================================================
     * Support
     * ================================================================= */
    private AstNode child(ParseTree ctx)
    {
        return ctx == null ? null : visit(ctx);
    }

    private Expression expression(ParseTree ctx)
    {
        return child(ctx) instanceof Expression expression ? expression : null;
    }

    private Block block(ParseTree ctx)
    {
        return child(ctx) instanceof Block block ? block : null;
    }

    private List<AstNode> statements(List<LatinParser.InstruccionContext> contexts)
    {
        List<AstNode> statements = new ArrayList<>();

        for (LatinParser.InstruccionContext statement : contexts)
        {
            add(statements, visit(statement));
        }
        return statements;
    }

    private List<Expression> expressionList(LatinParser.ListaExpresionesContext ctx)
    {
        List<Expression> values = new ArrayList<>();

        if (ctx != null)
        {
            for (LatinParser.ExpresionContext item : ctx.expresion())
            {
                Expression value = expression(item);
                if (value != null)
                {
                    values.add(value);
                }
            }
        }
        return values;
    }

    private List<Parameter> parameters(LatinParser.ListaParametrosContext ctx)
    {
        List<Parameter> declared = new ArrayList<>();

        if (ctx != null)
        {
            for (LatinParser.ParametroContext parameter : ctx.parametro())
            {
                if (visit(parameter) instanceof Parameter declaration)
                {
                    declared.add(declaration);
                }
            }
        }
        return declared;
    }

    private void add(List<AstNode> target, AstNode node)
    {
        if (node != null)
        {
            target.add(node);
        }
    }

    private int line(ParserRuleContext ctx)
    {
        Token token = ctx.getStart();
        return token == null ? 0 : token.getLine();
    }

    private int column(ParserRuleContext ctx)
    {
        Token token = ctx.getStart();
        return token == null ? 0 : token.getCharPositionInLine() + 1;
    }
}
