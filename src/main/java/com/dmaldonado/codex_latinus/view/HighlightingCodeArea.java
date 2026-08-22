package com.dmaldonado.codex_latinus.view;

import com.dmaldonado.codex_latinus.grammar.LatinLexer;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

/**
 * The editor colours the code with the REAL LatinLexer.
 *
 * The lecturer required the colouring to come out of the project's own lexical
 * analysis and not out of a generic plugin (Telegram, 12/08): RichTextFX only
 * paints the spans, every decision about what is a reserved word is taken by
 * the same lexer the compiler runs. That is why this is the one class of the
 * view that imports ANTLR -- and the token TYPE is what is matched, never the
 * text of the word, so a hand-written copy of the lexer can never drift from it.
 */
public final class HighlightingCodeArea
{
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
                codeArea.setStyleSpans(0, computeHighlighting(newText)));

        return codeArea;
    }

    /**
     * Package private so the self check of this phase can call it directly: the
     * invariant that setStyleSpans demands -- the spans have to cover the text
     * exactly -- is worth verifying without opening a window.
     *
     * ponytail: the whole document is lexed again on every keystroke. ANTLR is
     * far faster than typing at the file sizes of this practice; if it ever
     * lags, drive it from codeArea.multiPlainChanges().successionEnds(...).
     */
    static StyleSpans<Collection<String>> computeHighlighting(String text)
    {
        // StyleSpansBuilder.create() throws when no span was added, and an empty
        // editor is the very first state of the application.
        if (text.isEmpty())
        {
            return StyleSpans.<Collection<String>>singleton(Collections.emptyList(), 0);
        }

        LatinLexer lexer = new LatinLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();   // half typed code is the normal state here

        StyleSpansBuilder<Collection<String>> builder = new StyleSpansBuilder<>();
        int lastEnd = 0;

        // getAllTokens() keeps the HIDDEN channel, which is exactly why the
        // grammar sends comments and blanks there instead of skipping them:
        // they get painted and no hole is left in the middle of the text.
        for (Token token : lexer.getAllTokens())
        {
            int start = token.getStartIndex();
            int end   = token.getStopIndex() + 1;

            if (start > lastEnd)
            {
                builder.add(Collections.emptyList(), start - lastEnd);
            }
            builder.add(Collections.singleton(styleClassFor(token.getType())), end - start);
            lastEnd = end;
        }

        if (lastEnd < text.length())
        {
            builder.add(Collections.emptyList(), text.length() - lastEnd);
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
            case LatinLexer.CARACTER_INVALIDO                    -> "invalid";
            default                                              -> "plain";
        };
    }
}
