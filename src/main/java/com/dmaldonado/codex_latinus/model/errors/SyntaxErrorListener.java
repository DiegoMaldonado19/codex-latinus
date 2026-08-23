package com.dmaldonado.codex_latinus.model.errors;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

/**
 * Reemplaza al ConsoleErrorListener de ANTLR: en vez de imprimir en ingles a
 * System.err, cada error de sintaxis va al ErrorManager en espanol y con la
 * columna 1-based.
 *
 * No existe su equivalente lexico: ver LatinCompiler.reportInvalidCharacters.
 */
public class SyntaxErrorListener extends BaseErrorListener
{
    private final ErrorManager errorManager;

    public SyntaxErrorListener(ErrorManager errorManager)
    {
        this.errorManager = errorManager;
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String message,
                            RecognitionException exception)
    {
        String lexeme = (offendingSymbol instanceof Token token)
                ? token.getText() : String.valueOf(offendingSymbol);

        errorManager.addSyntactic(translate(message), lexeme, line, charPositionInLine + 1);
    }

    /** ANTLR only speaks English; the interface has to read in Spanish. */
    private String translate(String message)
    {
        if (message == null)
        {
            return "Error de sintaxis.";
        }
        return message
                .replace("mismatched input", "no se esperaba")
                .replace("no viable alternative at input", "construccion invalida en")
                .replace("extraneous input", "sobra el token")
                .replace("expecting", "se esperaba")
                .replace("missing", "falta")
                .replace("alternative", "alternativa")
                // ANTLR appends "at '<token>'"; only that exact shape is
                // replaced, so the word "at" inside a lexeme is left alone.
                .replace(" at '", " en '");
    }
}
