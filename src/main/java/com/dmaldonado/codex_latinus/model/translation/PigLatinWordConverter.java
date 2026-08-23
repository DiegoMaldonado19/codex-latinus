package com.dmaldonado.codex_latinus.model.translation;

/**
 * Las leyes de conversion del enunciado. Algoritmo puro: sin estado, sin ANTLR
 * y sin AST, asi que se puede probar por si solo.
 *
 *   CONSONANTES: fuerza -> uerzafay      VOCALES: inicio -> inicioway
 *   PORCINA:     &lt;&lt; -> %OINK_OINK   &gt;&gt; -> %OINK
 *
 * Se convierte segmento a segmento para que sobrevivan '_' y digitos:
 * mi_variable2 -> imay_ariablevay2
 */
public final class PigLatinWordConverter
{
    public static final String INPUT_OINK  = "%OINK_OINK";
    public static final String OUTPUT_OINK = "%OINK";

    /** Con escapes: las fuentes van en ASCII y el lexer si acepta acentos. */
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
     * Aplica las leyes a un segmento de solo letras conservando el estilo:
     * calcularPoder -> alcularPodercay, Persona -> Ersonapay,
     * VARIABILES -> ARIABILESVAY.
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

    /** Convierte el contenido de un textum palabra por palabra, con sus comillas. */
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
