package com.dmaldonado.codex_latinus.model.errors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Single accumulator shared by the whole pipeline: the syntax listener, the
 * invalid character scan and the semantic analyzer all write here.
 *
 * ANTLR's default behaviour (print the first error to System.err in English and
 * carry on) is useless for this project: the statement wants every error listed
 * in the interface with line:column, and the pipeline has to decide where to
 * stop based on WHICH kind of errors appeared. Both need one owner of the list.
 */
public class ErrorManager
{
    private final List<CompilerError> errors = new ArrayList<>();

    private void add(CompilerError error)
    {
        errors.add(error);
    }

    public void addLexical(String description, String lexeme, int line, int column)
    {
        add(new CompilerError(ErrorType.LEXICAL, description, lexeme, line, column));
    }

    public void addSyntactic(String description, String lexeme, int line, int column)
    {
        add(new CompilerError(ErrorType.SYNTACTIC, description, lexeme, line, column));
    }

    public void addSemantic(String description, String lexeme, int line, int column)
    {
        add(new CompilerError(ErrorType.SEMANTIC, description, lexeme, line, column));
    }

    public boolean hasErrorsOf(ErrorType type)
    {
        return errors.stream().anyMatch(error -> error.getType() == type);
    }

    /** Sorted by position, which is the order the user reads the file in. */
    public List<CompilerError> getErrors()
    {
        List<CompilerError> sorted = new ArrayList<>(errors);
        sorted.sort(Comparator.comparingInt(CompilerError::getLine)
                              .thenComparingInt(CompilerError::getColumn));
        return Collections.unmodifiableList(sorted);
    }
}
