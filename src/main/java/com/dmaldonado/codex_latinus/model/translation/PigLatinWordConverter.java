package com.dmaldonado.codex_latinus.model.translation;

/**
 * The word conversion laws of the statement. Pure algorithm: no state, no
 * ANTLR, no AST, so it can be tested on its own.
 *
 *   LEY DE CONSONANTES: the leading consonant group moves to the end and "ay"
 *   is appended.        fuerza -> uerza + f + ay -> uerzafay
 *                       tabla  -> abla  + t + ay -> ablatay
 *
 *   LEY DE VOCALES: a word starting with a vowel just gets "way".
 *                       inicio  -> inicioway
 *                       archivo -> archivoway
 *
 *   LEY PORCINA:  <<  ->  %OINK_OINK        >>  ->  %OINK
 *
 * Identifiers are converted segment by segment so underscores and digits
 * survive:  mi_variable2 -> imay_ariablevay2
 */
public final class PigLatinWordConverter
{
    public static final String INPUT_OINK  = "%OINK_OINK";
    public static final String OUTPUT_OINK = "%OINK";

    /** Written with escapes because the rest of the project keeps its sources
     *  in ASCII, and the lexer does accept accented letters in identifiers. */
    private static final String VOWELS = "aeiou\u00e1\u00e9\u00ed\u00f3\u00fa\u00fc";

    private PigLatinWordConverter()
    {
    }

    /** Converts a whole identifier or reserved word, segment by segment. */
    public static String convert(String word)
    {
        if (word == null || word.isEmpty())
        {
            return word;
        }

        StringBuilder output  = new StringBuilder();
        StringBuilder segment = new StringBuilder();

        for (char character : word.toCharArray())
        {
            if (Character.isLetter(character))
            {
                segment.append(character);
            }
            else
            {
                output.append(convertSegment(segment.toString()));
                segment.setLength(0);
                output.append(character);   // '_' and digits are kept as they are
            }
        }
        output.append(convertSegment(segment.toString()));
        return output.toString();
    }

    /**
     * Applies the laws to a segment made only of letters, keeping the casing
     * style of the original identifier:
     *   calcularPoder -> alcularPodercay   (camelCase intact)
     *   Persona       -> Ersonapay         (PascalCase intact)
     *   VARIABILES    -> ARIABILESVAY      (section markers stay uppercase)
     */
    private static String convertSegment(String segment)
    {
        if (segment.isEmpty())
        {
            return segment;
        }

        boolean allUppercase   = segment.length() > 1 && segment.equals(segment.toUpperCase());
        boolean startsUppercase = Character.isUpperCase(segment.charAt(0));
        int     vowelIndex      = firstVowelIndex(segment.toLowerCase());

        String result;

        if (vowelIndex == 0)
        {
            result = segment + "way";                            // ley de vocales
        }
        else if (vowelIndex < 0)
        {
            result = segment + "ay";                             // word with no vowel
        }
        else
        {
            String body       = segment.substring(vowelIndex);   // ley de consonantes
            String consonants = segment.substring(0, vowelIndex);

            if (startsUppercase && !allUppercase)
            {
                body       = capitalize(body);
                consonants = consonants.toLowerCase();
            }
            result = body + consonants + "ay";
        }
        return allUppercase ? result.toUpperCase() : result;
    }

    private static int firstVowelIndex(String word)
    {
        for (int i = 0; i < word.length(); i++)
        {
            if (VOWELS.indexOf(word.charAt(i)) >= 0)
            {
                return i;
            }
        }
        return -1;
    }

    private static String capitalize(String word)
    {
        return word.isEmpty() ? word
                : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /** Converts the content of a textum word by word, keeping the quotes. */
    public static String convertText(String quotedLiteral)
    {
        if (quotedLiteral.length() < 2)
        {
            return quotedLiteral;
        }

        String        content = quotedLiteral.substring(1, quotedLiteral.length() - 1);
        StringBuilder output  = new StringBuilder("\"");

        for (String word : content.split(" ", -1))
        {
            output.append(convert(word)).append(' ');
        }

        if (output.length() > 1)
        {
            output.setLength(output.length() - 1);
        }
        return output.append('"').toString();
    }
}
