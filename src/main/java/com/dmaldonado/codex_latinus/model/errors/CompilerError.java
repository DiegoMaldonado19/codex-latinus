package com.dmaldonado.codex_latinus.model.errors;

/**
 * One reported error, with the line:column format the statement requires.
 *
 * Immutable on purpose: an error is a fact about a compilation that already
 * happened, nothing should be able to edit it afterwards.
 */
public class CompilerError {
    private final ErrorType type;
    private final String description;
    private final String lexeme;
    private final int line;
    private final int column;

    public CompilerError(ErrorType type, String description, String lexeme, int line, int column) {
        this.type = type;
        this.description = description;
        this.lexeme = lexeme == null ? "" : lexeme;
        this.line = line;
        this.column = column;
    }

    public ErrorType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getLexeme() {
        return lexeme;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "[" + type + "] linea " + line + ", columna " + column + ": " + description
                + (lexeme.isEmpty() ? "" : "  ('" + lexeme + "')");
    }
}
