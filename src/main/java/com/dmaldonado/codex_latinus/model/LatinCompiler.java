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
 * Runs the whole pipeline and enforces where it has to stop.
 *
 * The statement is explicit about the cut points: "Graficar el AST generado,
 * solo si el lenguaje es valido" and "Verificar la consistencia semantica del
 * codigo reportando cualquier anomalia antes de cualquier proceso de
 * transformacion". Stopping at the right step matters as much as each
 * individual check being correct.
 *
 *   lexer  -> scan for CARACTER_INVALIDO
 *   parser -> SyntaxErrorListener
 *      lexical or syntax errors ? -> stop, no AST is built
 *   ProcessStackListener -> the simulated stack, recorded even on a bad file
 *   AstBuilderVisitor -> Program
 *   SemanticAnalyzer  -> SymbolTable
 *      semantic errors ? -> stop, nothing is graphed or translated
 *
 * Knows nothing about the interface: it prints nothing and returns a result the
 * controller of module 10 paints.
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
        reportInvalidCharacters(tokens, errorManager);

        // A file with invalid characters is not parsed at all: every syntax
        // error the parser would report after one is noise derived from it.
        if (errorManager.hasErrorsOf(ErrorType.LEXICAL))
        {
            return new CompilationResult(null, symbolTable, List.of(), errorManager.getErrors());
        }

        LatinParser parser = new LatinParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new SyntaxErrorListener(errorManager));

        LatinParser.ProgramaContext parseTree = parser.programa();

        // Recorded before the syntax cut on purpose: a file that does not parse
        // is exactly the one whose ERROR steps the user needs to see.
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
     * There is no LexicalErrorListener, and that is deliberate: the last rule of
     * the lexer is "CARACTER_INVALIDO : . ;", a catch-all, so the lexer can
     * never fail to match and a BaseErrorListener on it would never fire. Any
     * character the language does not know lands on that token instead, and
     * this is where it is turned into a lexical error with its exact position.
     */
    private void reportInvalidCharacters(CommonTokenStream tokens, ErrorManager errorManager)
    {
        for (Token token : tokens.getTokens())
        {
            if (token.getType() == LatinLexer.CARACTER_INVALIDO)
            {
                errorManager.addLexical("Simbolo no reconocido por el lenguaje.",
                        token.getText(), token.getLine(), token.getCharPositionInLine() + 1);
            }
        }
    }
}
