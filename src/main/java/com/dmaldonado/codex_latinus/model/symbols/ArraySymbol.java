package com.dmaldonado.codex_latinus.model.symbols;

import com.dmaldonado.codex_latinus.model.types.DataType;

/**
 * An array declared with {@code series}.
 *
 * size is what makes the statement's rule possible: "Si la expresion se puede
 * evaluar al hacer la verificacion de semantica se debe lanzar un error en caso
 * de que se salga del rango declarado". It is -1 when the size is not a
 * constant the compiler can evaluate.
 *
 * elementStructName remembers that "series animales : Animal" holds Animal
 * instances, so that mi_selva.animales[1].nombre can be resolved.
 */
public class ArraySymbol extends Symbol
{
    private final int    size;
    private final int    valueCount;
    private final String elementStructName;

    public ArraySymbol(String name, DataType elementType, String typeText, String scopeName,
                       int size, int valueCount, String elementStructName, int line, int column)
    {
        super(name, elementType, typeText, SymbolCategory.ARRAY, scopeName, line, column);
        this.size              = size;
        this.valueCount        = valueCount;
        this.elementStructName = elementStructName;
    }

    public int getSize()
    {
        return size;
    }

    public String getElementStructName()
    {
        return elementStructName;
    }

    @Override
    public String getDetail()
    {
        return "tamano = " + (size < 0 ? "?" : size) + ", valores = " + valueCount;
    }
}
