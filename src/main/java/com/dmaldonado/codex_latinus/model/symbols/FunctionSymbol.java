package com.dmaldonado.codex_latinus.model.symbols;

import com.dmaldonado.codex_latinus.model.types.DataType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** A function declared with {@code actio} (no return) or {@code ratio}. */
public class FunctionSymbol extends Symbol
{
    private final List<VariableSymbol> parameters;
    private final boolean              returnsValue;
    /** Name of the structura when the return type is ESTRUCTURA. */
    private final String               returnStructName;

    public FunctionSymbol(String name, DataType returnType, String typeText, String scopeName,
                          List<VariableSymbol> parameters, boolean returnsValue,
                          String returnStructName, int line, int column)
    {
        super(name, returnType, typeText, SymbolCategory.FUNCTION, scopeName, line, column);
        this.parameters       = parameters == null ? new ArrayList<>() : parameters;
        this.returnsValue     = returnsValue;
        this.returnStructName = returnStructName;
    }

    public List<VariableSymbol> getParameters()
    {
        return parameters;
    }

    public boolean returnsValue()
    {
        return returnsValue;
    }

    public String getReturnStructName()
    {
        return returnStructName;
    }

    public int getParameterCount()
    {
        return parameters.size();
    }

    /** Signature used in error messages: calcularPoder(numerus, decimalis). */
    public String getSignature()
    {
        return getName() + "(" + parameters.stream()
                .map(Symbol::getTypeText)
                .collect(Collectors.joining(", ")) + ")";
    }

    @Override
    public String getDetail()
    {
        return getSignature();
    }
}
