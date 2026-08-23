package com.dmaldonado.codex_latinus.model;

import com.dmaldonado.codex_latinus.grammar.LatinLexer;
import com.dmaldonado.codex_latinus.grammar.LatinParser;
import com.dmaldonado.codex_latinus.model.analysis.AstBuilderVisitor;
import com.dmaldonado.codex_latinus.model.analysis.SemanticAnalyzer;
import com.dmaldonado.codex_latinus.model.ast.declaration.Program;
import com.dmaldonado.codex_latinus.model.errors.CompilerError;
import com.dmaldonado.codex_latinus.model.errors.ErrorManager;
import com.dmaldonado.codex_latinus.model.errors.ErrorType;
import com.dmaldonado.codex_latinus.model.errors.SyntaxErrorListener;
import com.dmaldonado.codex_latinus.model.stack.ProcessStackListener;
import com.dmaldonado.codex_latinus.model.stack.ProcessStep;
import com.dmaldonado.codex_latinus.model.symbols.SymbolTable;
import java.util.List;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Corre el pipeline completo y decide donde cortar, que el enunciado exige tanto
 * como cada validacion: nada se grafica ni se traduce si hay errores.
 *
 * No sabe nada de la interfaz: no imprime, devuelve un CompilationResult.
 *
 * Diagrama del pipeline: docs/05-Manual-Tecnico.md
 */
public class LatinCompiler
{
    /**
     * @param ast          null when the source did not get past the parser.
     * @param symbolTable  empty when the AST was never built.
     * @param steps        empty only when the file was never parsed at all.
     */
    public record CompilationResult(Program ast, SymbolTable symbolTable,
                                    List<ProcessStep> steps, List<CompilerError> errors)
    {
        /** Only a valid program may be graphed and translated. */
        public boolean isValid()
        {
            return errors.isEmpty() && ast != null;
        }
    }

    public CompilationResult compile(String source)
    {
        ErrorManager errorManager = new ErrorManager();
        SymbolTable  symbolTable  = new SymbolTable();

        LatinLexer lexer = new LatinLexer(CharStreams.fromString(source == null ? "" : source));
        lexer.removeErrorListeners();   // silence ANTLR's ConsoleErrorListener

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        tokens.fill();
        reportLexicalErrors(tokens, errorManager);

        // Con errores lexicos no se parsea: todo error de sintaxis posterior
        // seria ruido derivado de este.
        if (errorManager.hasErrorsOf(ErrorType.LEXICAL))
        {
            return new CompilationResult(null, symbolTable, List.of(), errorManager.getErrors());
        }

        LatinParser parser = new LatinParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(errorManager));

        LatinParser.ProgramaContext parseTree = parser.programa();

        // Antes del corte por sintaxis a proposito: el archivo que no parsea es
        // justo aquel cuyos pasos ERROR el usuario necesita ver.
        ProcessStackListener stackListener = new ProcessStackListener(parser);
        ParseTreeWalker.DEFAULT.walk(stackListener, parseTree);
        List<ProcessStep> steps = stackListener.getSteps();

        if (errorManager.hasErrorsOf(ErrorType.SYNTACTIC))
        {
            return new CompilationResult(null, symbolTable, steps, errorManager.getErrors());
        }

        Program ast = new AstBuilderVisitor().build(parseTree);

        if (ast != null)
        {
            new SemanticAnalyzer(errorManager, symbolTable).visitProgram(ast);
        }
        return new CompilationResult(ast, symbolTable, steps, errorManager.getErrors());
    }

    /**
     * No hay LexicalErrorListener a proposito: las reglas de error del lexer
     * cubren todo lo que no encaja, la ultima de ellas con "CARACTER_INVALIDO
     * : . ;", asi que el lexer nunca falla y un BaseErrorListener sobre el
     * jamas dispararia. El mensaje sale del tipo de token, no del texto.
     */
    private void reportLexicalErrors(CommonTokenStream tokens, ErrorManager errorManager)
    {
        for (Token token : tokens.getTokens())
        {
            String description = switch (token.getType())
            {
                case LatinLexer.TEXTO_SIN_CERRAR ->
                        "Cadena sin cerrar: falta la comilla doble final.";
                case LatinLexer.CARACTER_SIN_CERRAR ->
                        "Caracter sin cerrar: falta la comilla simple final.";
                case LatinLexer.COMENTARIO_SIN_CERRAR ->
                        "Comentario de bloque sin cerrar: falta '*/'.";
                case LatinLexer.CARACTER_INVALIDO ->
                        "Simbolo no reconocido por el lenguaje.";
                default -> null;
            };

            if (description != null)
            {
                errorManager.addLexical(description, token.getText(),
                        token.getLine(), token.getCharPositionInLine() + 1);
            }
        }
    }
}
