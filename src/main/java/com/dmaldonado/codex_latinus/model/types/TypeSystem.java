package com.dmaldonado.codex_latinus.model.types;

/**
 * Reglas de compatibilidad de tipos. Todo metodo devuelve ERROR cuando la
 * combinacion es invalida, y el analizador sigue caminando con ese ERROR.
 *
 * La tabla completa es el entregable docs/03-Tabla-de-Tipos.md
 */
public final class TypeSystem
{
    private TypeSystem()
    {
    }

    /* ================= Arithmetic: + - * / ================= */

    public static DataType arithmeticResult(DataType left, DataType right, String operator)
    {
        if (left == DataType.ERROR || right == DataType.ERROR)
        {
            return DataType.ERROR;
        }

        // textum only takes part through '+' (concatenation), with any type.
        if (left == DataType.TEXTUM || right == DataType.TEXTUM)
        {
            return "+".equals(operator) ? DataType.TEXTUM : DataType.ERROR;
        }
        if (left == DataType.BOOLEANO || right == DataType.BOOLEANO)
        {
            return DataType.ERROR;
        }
        if (!left.isNumeric() || !right.isNumeric())
        {
            return DataType.ERROR;
        }

        // Highest rank wins: littera < numerus < decimalis
        return left.getRank() >= right.getRank() ? left : right;
    }

    /* ================= Relational: == != < > <= >= ================= */

    public static DataType relationalResult(DataType left, DataType right, String operator)
    {
        if (left == DataType.ERROR || right == DataType.ERROR)
        {
            return DataType.ERROR;
        }

        boolean isEquality = "==".equals(operator) || "!=".equals(operator);

        // == and != work between equal types, textum and bool included.
        if (isEquality && left == right && left != DataType.ESTRUCTURA)
        {
            return DataType.BOOLEANO;
        }
        // < > <= >= only between numeric types.
        if (left.isNumeric() && right.isNumeric())
        {
            return DataType.BOOLEANO;
        }
        return DataType.ERROR;
    }

    /* ================= Logical: && || non ================= */

    public static DataType logicalResult(DataType left, DataType right)
    {
        return (left == DataType.BOOLEANO && right == DataType.BOOLEANO)
                ? DataType.BOOLEANO : DataType.ERROR;
    }

    public static DataType negationResult(DataType operand)
    {
        return operand == DataType.BOOLEANO ? DataType.BOOLEANO : DataType.ERROR;
    }

    public static DataType unaryMinusResult(DataType operand)
    {
        return operand.isNumeric() ? operand : DataType.ERROR;
    }

    /** ++ and -- only apply to numerus / decimalis. */
    public static DataType incrementResult(DataType operand)
    {
        return (operand == DataType.NUMERUS || operand == DataType.DECIMALIS)
                ? operand : DataType.ERROR;
    }

    /* ================= Assignment ================= */

    /**
     * Puede guardarse un valor {@code source} en un destino {@code target}?
     * Solo se ensancha (littera -> numerus -> decimalis), nunca se estrecha.
     *
     * Dos structuras distintas son ambas ESTRUCTURA aqui: comparar el NOMBRE le
     * toca a SemanticAnalyzer.assignable, que es quien los conoce.
     */
    public static boolean isAssignable(DataType target, DataType source)
    {
        if (target == DataType.ERROR || source == DataType.ERROR)
        {
            return true;   // an error was already reported: do not duplicate it
        }
        if (target == source)
        {
            return true;
        }
        if (target == DataType.DECIMALIS)
        {
            return source == DataType.NUMERUS || source == DataType.LITTERA;
        }
        if (target == DataType.NUMERUS)
        {
            return source == DataType.LITTERA;
        }
        return false;
    }
}
