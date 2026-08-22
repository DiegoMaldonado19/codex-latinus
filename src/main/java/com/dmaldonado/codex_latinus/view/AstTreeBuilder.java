package com.dmaldonado.codex_latinus.view;

import com.dmaldonado.codex_latinus.model.ast.AstNode;
import javafx.scene.control.TreeItem;

/**
 * Turns any AST into a JavaFX tree without knowing a single concrete node type:
 * getLabel() and getChildren() of AstNode are the whole contract.
 *
 * Every item is expanded on the way out because the statement asks for the AST
 * to be GRAPHED, not for the user to unfold it node by node.
 */
public final class AstTreeBuilder
{
    private AstTreeBuilder()
    {
    }

    public static TreeItem<String> build(AstNode node)
    {
        TreeItem<String> item = new TreeItem<>(node.getLabel());

        for (AstNode child : node.getChildren())
        {
            item.getChildren().add(build(child));
        }
        item.setExpanded(true);
        return item;
    }
}
