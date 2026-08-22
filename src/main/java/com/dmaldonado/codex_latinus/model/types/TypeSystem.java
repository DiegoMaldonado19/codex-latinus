package com.dmaldonado.codex_latinus.model.types;

/**
 * Type compatibility rules of the Latin language.
 *
 * Direct translation of the statement:
 *   "Siempre se considera la jerarquia mas alta de tipos para el resultado.
 *    El unico tipo de dato incompatible con todas las operaciones es el textum,
 *    el cual solo se puede concatenar con los otros (con una suma)."
 *
 * Every method returns DataType.ERROR when the combination is invalid. The
 * semantic analyzer reports it once and keeps walking with ERROR, so a single
 * mistake does not print ten messages.
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
     * Can a value of type {@code source} be stored in a variable declared as
     * {@code target}?
     *
     * This table is the "Tabla de compatibilidad de tipos" the statement asks
     * for as a documentation deliverable.
     *
     *  target \ source | numerus | decimalis | textum | littera | bool
     *  ----------------+---------+-----------+--------+---------+------
     *  numerus         |   SI    |    NO     |   NO   |   SI    |  NO
     *  decimalis       |   SI    |    SI     |   NO   |   SI    |  NO
     *  textum          |   NO    |    NO     |   SI   |   NO    |  NO
     *  littera         |   NO    |    NO     |   NO   |   SI    |  NO
     *  bool            |   NO    |    NO     |   NO   |   NO    |  SI
     *
     * Two different structuras are both ESTRUCTURA here, so this method says
     * yes to them. Comparing the structure NAME is the analyzer's job
     * (SemanticAnalyzer.assignable), because only it knows the names.
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
