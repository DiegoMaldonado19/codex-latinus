package com.dmaldonado.codex_latinus.view;

import com.dmaldonado.codex_latinus.grammar.LatinLexer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * El editor colorea con el LatinLexer REAL. El catedratico exigio que el color
 * saliera del analisis lexico propio y no de un plugin (Telegram 12/08):
 * RichTextFX solo pinta los spans.
 *
 * Por eso es la unica clase de la vista que importa ANTLR, y lo que compara es
 * el TIPO de token, nunca el texto de la palabra.
 */
public final class HighlightingCodeArea
{
    private static final Logger LOGGER = Logger.getLogger(HighlightingCodeArea.class.getName());

    private static final Set<Integer> KEYWORD_TOKENS = Set.of(
        LatinLexer.VARIABILES, LatinLexer.VARIABILES_LOCAL, LatinLexer.MUNERA,
        LatinLexer.MAIOR,      LatinLexer.FIN_PROGRAMA,     LatinLexer.ESTO,
        LatinLexer.SERIES,     LatinLexer.STRUCTURA,        LatinLexer.FINIS,
        LatinLexer.SI,         LatinLexer.ALITER,           LatinLexer.DUM,
        LatinLexer.FACERE,     LatinLexer.PER,              LatinLexer.PERGE,
        LatinLexer.INTERRUMPE, LatinLexer.ACTIO,            LatinLexer.RATIO,
        LatinLexer.REDDERE,    LatinLexer.NON
    );

    private static final Set<Integer> TYPE_TOKENS = Set.of(
        LatinLexer.NUMERUS, LatinLexer.DECIMALIS, LatinLexer.TEXTUM,
        LatinLexer.LITTERA, LatinLexer.BOOL,      LatinLexer.VERUM,
        LatinLexer.FALSUS
    );

    private HighlightingCodeArea()
    {
    }

    public static CodeArea create()
    {
        CodeArea codeArea = new CodeArea();

        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.getStyleClass().add("latin-editor");
        codeArea.textProperty().addListener((observable, oldText, newText) ->
                applyHighlighting(codeArea, newText));

        return codeArea;
    }

    /**
     * Regla 6 de CLAUDE.md. Sin este try/catch una excepcion aqui muere dentro
     * del listener de textProperty: el editor deja de colorear en cada tecla y
     * no queda rastro de por que. Se registra con la excepcion completa (que
     * lleva mensaje, archivo y numero de linea) y el texto se queda sin estilo
     * en vez de dejar el editor mudo.
     */
    private static void applyHighlighting(CodeArea codeArea, String text)
    {
        try
        {
            codeArea.setStyleSpans(0, computeHighlighting(text));
        }
        catch (RuntimeException exception)
        {
            LOGGER.log(Level.SEVERE, "No se pudo colorear el texto del editor.", exception);
        }
    }

    /**
     * Package private para que el self check lo llame sin abrir una ventana:
     * setStyleSpans exige que los spans cubran el texto EXACTAMENTE.
     *
     * ponytail: se re-lexa el documento entero en cada tecla. Si alguna vez se
     * siente lento, alimentarlo desde codeArea.multiPlainChanges().
     */
    static StyleSpans<Collection<String>> computeHighlighting(String text)
    {
        // create() lanza si no se agrego ni un span, y el editor arranca vacio.
        if (text.isEmpty())
        {
            return StyleSpans.<Collection<String>>singleton(Collections.emptyList(), 0);
        }

        LatinLexer lexer = new LatinLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();   // half typed code is the normal state here

        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();

        // getAllTokens() conserva el canal HIDDEN: por eso la gramatica manda
        // comentarios y espacios ahi en vez de descartarlos con skip. Y como
        // ninguna regla usa skip, el flujo es contiguo y cubre todo el texto.
        //
        // La longitud sale del texto del token y NO de stopIndex - startIndex:
        // CharStreams.fromString indexa por puntos de codigo y String por
        // chars, asi que con un emoji los indices del lexer se desalinean y
        // todo lo que va detras se colorearia corrido.
        for (Token token : lexer.getAllTokens())
        {
            builder.add(Collections.singleton(styleClassFor(token.getType())),
                        token.getText().length());
        }
        return builder.create();
    }

    private static String styleClassFor(int tokenType)
    {
        if (KEYWORD_TOKENS.contains(tokenType))
        {
            return "keyword";
        }
        if (TYPE_TOKENS.contains(tokenType))
        {
            return "type";
        }

        return switch (tokenType)
        {
            case LatinLexer.ENTERO, LatinLexer.DECIMAL           -> "number";
            case LatinLexer.TEXTO, LatinLexer.CARACTER           -> "string";
            case LatinLexer.COMENTARIO_LINEA,
                 LatinLexer.COMENTARIO_BLOQUE                    -> "comment";
            case LatinLexer.TEXTO_SIN_CERRAR,
                 LatinLexer.CARACTER_SIN_CERRAR,
                 LatinLexer.COMENTARIO_SIN_CERRAR,
                 LatinLexer.CARACTER_INVALIDO                    -> "invalid";
            default                                              -> "plain";
        };
    }
}
