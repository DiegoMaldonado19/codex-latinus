package com.dmaldonado.codex_latinus.model.analysis;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import com.dmaldonado.codex_latinus.model.ast.expression.LiteralExpression;
import com.dmaldonado.codex_latinus.model.ast.statement.Block;
import com.dmaldonado.codex_latinus.model.ast.statement.BreakStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ContinueStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.DoWhileStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.IfStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.ReturnStatement;
import com.dmaldonado.codex_latinus.model.ast.statement.WhileStatement;
import java.util.List;

/**
 * Checks the two rules the statement demands of a ratio function: "Que se
 * retorne un valor en todos los caminos posibles y que no quede codigo que no
 * se puede rastrear".
 *
 * Static and conservative: it never executes anything, it only looks at the
 * SHAPE of the AST. When in doubt it answers "does not always return", which
 * makes it report a possible problem rather than stay silent about a real one.
 *
 * The aliter chain needs no special case here: phase 1 desugars it into nested
 * IfStatement nodes, so the elseBranch of an if is simply another if and the
 * recursion already covers it.
 */
public final class ReturnPathChecker
{
    private ReturnPathChecker()
    {
    }

    /** Does this node guarantee a reddere on EVERY execution path? */
    public static boolean alwaysReturns(AstNode node)
    {
        if (node == null)
        {
            return false;
        }
        if (node instanceof ReturnStatement)
        {
            return true;
        }
        if (node instanceof Block block)
        {
            return block.getStatements().stream().anyMatch(ReturnPathChecker::alwaysReturns);
        }
        if (node instanceof IfStatement ifStatement)
        {
            // Only guaranteed when there IS an aliter and BOTH branches return.
            return ifStatement.getElseBranch() != null
                && alwaysReturns(ifStatement.getThenBranch())
                && alwaysReturns(ifStatement.getElseBranch());
        }
        if (node instanceof DoWhileStatement doWhile)
        {
            return alwaysReturns(doWhile.getBody());   // facere runs at least once
        }
        if (node instanceof WhileStatement whileStatement)
        {
            // dum / per guarantee nothing, unless the condition is literally verum.
            return isAlwaysTrue(whileStatement.getCondition())
                && alwaysReturns(whileStatement.getBody());
        }
        return false;
    }

    private static boolean isAlwaysTrue(AstNode condition)
    {
        return condition instanceof LiteralExpression literal
            && "verum".equals(literal.getText());
    }

    /**
     * Index of the first unreachable statement inside a block, the one right
     * after a reddere / interrumpe / perge, or -1 when there is none.
     */
    public static int firstUnreachableIndex(Block block)
    {
        List<AstNode> statements = block.getStatements();

        for (int i = 0; i < statements.size() - 1; i++)
        {
            AstNode current = statements.get(i);

            if (current instanceof ReturnStatement
             || current instanceof BreakStatement
             || current instanceof ContinueStatement)
            {
                return i + 1;
            }
        }
        return -1;
    }
}
