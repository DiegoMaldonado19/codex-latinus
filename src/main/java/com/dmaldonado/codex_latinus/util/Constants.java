package com.dmaldonado.codex_latinus.util;

/**
 * Values shared by the view and the file dialogs.
 *
 * The palette of the editor is NOT here: it lives in codex-latinus.css, because
 * the one thing that reads it is the style sheet of the CodeArea.
 */
public final class Constants
{
    public static final String SOURCE_EXTENSION     = "lat";
    public static final String TRANSLATED_EXTENSION = "pig";
    public static final String APP_TITLE            = "Codex Latinus  -  Compilador Latin a PigLatin";

    private Constants()
    {
    }
}
