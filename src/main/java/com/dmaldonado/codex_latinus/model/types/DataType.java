package com.dmaldonado.codex_latinus.model.types;

/**
 * Primitive types of the Latin language, plus the markers the compiler needs
 * for things that are not primitive.
 *
 * The implicit conversion hierarchy (textum 5, decimalis 4, numerus 3,
 * littera 2, bool 1) belongs to the type checker, not here.
 */
public enum DataType
{
    NUMERUS,
    DECIMALIS,
    TEXTUM,
    LITTERA,
    BOOLEANO,
    ESTRUCTURA,
    VOID,
    ERROR;

    /**
     * Maps the type written in the source to its DataType. "bool" is the
     * explicit boolean type; "verum"/"falsus" are the older form the language
     * still accepts as a type. Anything else is a user defined structure.
     */
    public static DataType fromText(String text)
    {
        if (text == null)
        {
            return ERROR;
        }

        return switch (text)
        {
            case "numerus"                   -> NUMERUS;
            case "decimalis"                 -> DECIMALIS;
            case "textum"                    -> TEXTUM;
            case "littera"                   -> LITTERA;
            case "bool", "verum", "falsus"   -> BOOLEANO;
            default                          -> ESTRUCTURA;
        };
    }
}
