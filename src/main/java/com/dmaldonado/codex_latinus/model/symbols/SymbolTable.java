package com.dmaldonado.codex_latinus.model.symbols;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tabla de simbolos con ambitos anidados. Guarda TRES vistas de los mismos
 * datos, para tres consumidores:
 *
 *  - currentScope: la cadena viva, para el analizador (cerrar oculta locales);
 *  - allSymbols:   historial plano, para graficar cuando ya todo esta cerrado;
 *  - structs/functions: resolucion por nombre, lo que permite el uso adelantado.
 */
public class SymbolTable
{
    private final Scope                       globalScope  = new Scope("global", null);
    private Scope                             currentScope = globalScope;
    private final List<Symbol>                allSymbols   = new ArrayList<>();
    private final Map<String, StructSymbol>   structs      = new LinkedHashMap<>();
    private final Map<String, FunctionSymbol> functions    = new LinkedHashMap<>();

    /* ----------------- Scopes ----------------- */

    public void openScope(String name)
    {
        currentScope = new Scope(name, currentScope);
    }

    public void closeScope()
    {
        if (currentScope.getParent() != null)
        {
            currentScope = currentScope.getParent();
        }
    }

    public String getCurrentScopeName()
    {
        return currentScope.getName();
    }

    /* ----------------- Declaration and lookup ----------------- */

    /** @return false if the identifier already exists in the CURRENT scope. */
    public boolean declare(Symbol symbol)
    {
        boolean added = currentScope.declare(symbol);

        if (added)
        {
            allSymbols.add(symbol);

            if (symbol instanceof StructSymbol struct)
            {
                structs.put(struct.getName(), struct);
            }
            else if (symbol instanceof FunctionSymbol function)
            {
                functions.put(function.getName(), function);
            }
        }
        return added;
    }

    public Symbol lookup(String name)
    {
        return currentScope.lookup(name);
    }

    public Symbol lookupLocal(String name)
    {
        return currentScope.lookupLocal(name);
    }

    public StructSymbol lookupStruct(String name)
    {
        return structs.get(name);
    }

    public FunctionSymbol lookupFunction(String name)
    {
        return functions.get(name);
    }

    /* ----------------- Reports ----------------- */

    public List<Symbol> getAllSymbols()
    {
        return allSymbols;
    }

}
