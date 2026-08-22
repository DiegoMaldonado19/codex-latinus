package com.dmaldonado.codex_latinus.model.stack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Records the parser's process stack so the interface can replay it step by
 * step with Anterior / Siguiente buttons.
 *
 * ANTLR4 is LL(*), top-down: there is no LR table stack to read. The stack the
 * user sees is built here, reading the events of the tree walk as if they were
 * the operations the statement asks for:
 *
 *   PUSH   -> a rule is entered and stays active
 *   SHIFT  -> a token of the input is consumed
 *   REDUCE -> the rule ends and is replaced by its node
 *   ACCEPT -> the start rule is reduced with the stack empty
 *   ERROR  -> an unexpected token
 *
 * It is driven by ParseTreeWalker over the tree that is ALREADY built, not by
 * parser.addParseListener(): during the parse itself, ALL(*) prediction can try
 * a rule and back out of it, and error recovery can re-enter one, so the same
 * step would show up twice or out of order in the panel.
 *
 * Implements the runtime's ParseTreeListener, not the one ANTLR generates, so
 * it keeps working with <listener>false</listener> in the pom.
 */
public class ProcessStackListener implements ParseTreeListener
{
    private final Parser            parser;
    private final List<ProcessStep> steps = new ArrayList<>();
    private final Deque<String>     stack = new ArrayDeque<>();
    private int counter;

    public ProcessStackListener(Parser parser)
    {
        this.parser = parser;
    }

    public List<ProcessStep> getSteps()
    {
        return steps;
    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx)
    {
        String rule = parser.getRuleNames()[ctx.getRuleIndex()];

        stack.push(rule);
        record(StepAction.PUSH, "Se activa la regla <" + rule + ">", ctx.getStart());
    }

    /**
     * getSymbolicName returns null for EOF, which is a real terminal of the
     * tree because the rule "programa" ends in it.
     */
    @Override
    public void visitTerminal(TerminalNode node)
    {
        Token  token = node.getSymbol();
        String name  = parser.getVocabulary().getSymbolicName(token.getType());

        record(StepAction.SHIFT, "Se consume el token " + (name == null ? "EOF" : name)
                + "  ->  '" + token.getText() + "'", token);
    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx)
    {
        String rule = parser.getRuleNames()[ctx.getRuleIndex()];

        if (!stack.isEmpty())
        {
            stack.pop();
        }
        record(StepAction.REDUCE, "Se reduce <" + rule + "> a un nodo del arbol", stop(ctx));

        // "sin errores pendientes": ANTLR recovers from a syntax error and
        // still finishes the tree, so reducing the start rule is not enough to
        // accept. The parse is over by now, so the count is already final.
        if (stack.isEmpty() && "programa".equals(rule) && parser.getNumberOfSyntaxErrors() == 0)
        {
            record(StepAction.ACCEPT, "Cadena aceptada por la gramatica", stop(ctx));
        }
    }

    @Override
    public void visitErrorNode(ErrorNode node)
    {
        record(StepAction.ERROR, "Token inesperado: '" + node.getText() + "'", node.getSymbol());
    }

    /**
     * The stack is copied on every step on purpose: that is what lets the panel
     * jump to any step without replaying the parse.
     *
     * ArrayDeque iterates from the top, so it is reversed to store it base
     * first, which is the order formattedStack() expects.
     */
    private void record(StepAction action, String detail, Token token)
    {
        steps.add(new ProcessStep(++counter, action, detail,
                new ArrayList<>(stack).reversed(),
                token == null ? 0 : token.getLine(),
                token == null ? 0 : token.getCharPositionInLine() + 1));
    }

    /** A rule with no tokens of its own has no stop token, only a start one. */
    private Token stop(ParserRuleContext ctx)
    {
        return ctx.getStop() != null ? ctx.getStop() : ctx.getStart();
    }
}
