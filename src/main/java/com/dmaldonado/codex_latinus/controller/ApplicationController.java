package com.dmaldonado.codex_latinus.controller;

import com.dmaldonado.codex_latinus.model.LatinCompiler;
import com.dmaldonado.codex_latinus.model.LatinCompiler.CompilationResult;
import com.dmaldonado.codex_latinus.model.errors.CompilerError;
import com.dmaldonado.codex_latinus.model.translation.PigLatinTranslator;
import com.dmaldonado.codex_latinus.util.Constants;
import com.dmaldonado.codex_latinus.view.AstTreeBuilder;
import com.dmaldonado.codex_latinus.view.MainView;
import java.nio.file.Path;
import javafx.scene.control.TableRow;
import javafx.stage.Stage;
import org.fxmisc.richtext.CodeArea;

/**
 * THE BRIDGE (MVC).
 *
 * The only place where the view and the model meet. The view knows nothing
 * about ANTLR and LatinCompiler knows nothing about JavaFX; this class wires
 * one to the other and is the single place where the buttons are given a
 * meaning.
 *
 * There is no CompilerController wrapping LatinCompiler: the model already runs
 * the whole pipeline and already returns a UI agnostic CompilationResult, so a
 * wrapper would only duplicate that record.
 */
public class ApplicationController
{
    private final MainView           view;
    private final Stage              stage;
    private final LatinCompiler      compiler   = new LatinCompiler();
    private final PigLatinTranslator translator = new PigLatinTranslator();
    private final FileController     files;

    private CompilationResult lastResult;

    public ApplicationController(MainView view, Stage stage)
    {
        this.view  = view;
        this.stage = stage;
        this.files = new FileController(stage);

        registerEvents();
        showFileName();
    }

    private void registerEvents()
    {
        view.getCompileButton().setOnAction(event -> compile());
        view.getNewButton().setOnAction(event -> newFile());
        view.getOpenButton().setOnAction(event -> open());
        view.getSaveButton().setOnAction(event -> save());
        view.getExportButton().setOnAction(event -> exportTranslation());

        // Double click on an error moves the caret to it: three lines that pay
        // for themselves the first time a file has more than one error.
        view.getErrorTable().setRowFactory(table ->
        {
            TableRow<CompilerError> row = new TableRow<>();

            row.setOnMouseClicked(event ->
            {
                if (event.getClickCount() == 2 && !row.isEmpty())
                {
                    jumpTo(row.getItem());
                }
            });
            return row;
        });

        view.getTranslateKeywordsBox().setOnAction(event -> refreshTranslation());
        view.getTranslateStringsBox().setOnAction(event -> refreshTranslation());
    }

    /* ================================================================
     * Compilation
     * ================================================================ */

    private void compile()
    {
        lastResult = compiler.compile(view.getCodeArea().getText());

        view.showErrors(lastResult.errors());
        view.showSymbols(lastResult.symbolTable().getAllSymbols());

        // The statement forbids graphing and translating a program with errors,
        // so both panels are cleared instead of keeping a stale result on screen.
        view.showAst(lastResult.isValid() ? AstTreeBuilder.build(lastResult.ast()) : null);
        view.showTranslation(lastResult.isValid() ? translate() : "");
        view.getExportButton().setDisable(!lastResult.isValid());

        // The stack is shown whether the file compiled or not: an invalid file is
        // exactly the one whose ERROR steps the user needs to read.
        view.getProcessStackPanel().load(lastResult.steps());

        view.setStatus(summaryOf(lastResult), lastResult.isValid());
        view.showResultTab(lastResult.isValid());
    }

    /**
     * The checkboxes do not recompile anything: the AST of the last successful
     * compilation is still in memory, and translating it again is the whole job.
     */
    private void refreshTranslation()
    {
        if (lastResult != null && lastResult.isValid())
        {
            view.showTranslation(translate());
        }
    }

    private String translate()
    {
        translator.setTranslateKeywords(view.getTranslateKeywordsBox().isSelected());
        translator.setTranslateStrings(view.getTranslateStringsBox().isSelected());
        return translator.translate(lastResult.ast());
    }

    private static String summaryOf(CompilationResult result)
    {
        String steps = result.steps().size() + " paso(s) de analisis";

        if (result.isValid())
        {
            return "Compilacion exitosa  |  " + result.symbolTable().getAllSymbols().size()
                 + " simbolo(s)  |  " + steps;
        }
        return "Compilacion con " + result.errors().size() + " error(es)  |  " + steps;
    }

    private void jumpTo(CompilerError error)
    {
        CodeArea codeArea = view.getCodeArea();

        if (codeArea.getParagraphs().isEmpty())
        {
            return;
        }

        // The error is 1 based and the editor 0 based, and the position has to be
        // clamped: an error reported at the end of the file can carry a column
        // past the end of its line, and moveTo would throw on it.
        int paragraph = Math.max(0, Math.min(error.getLine() - 1, codeArea.getParagraphs().size() - 1));
        int column    = Math.max(0, Math.min(error.getColumn() - 1, codeArea.getParagraphLength(paragraph)));

        codeArea.moveTo(paragraph, column);
        codeArea.requestFollowCaret();
        codeArea.requestFocus();
    }

    /* ================================================================
     * Files
     * ================================================================ */

    private void newFile()
    {
        files.newFile();
        view.getCodeArea().clear();
        view.setStatus("Nuevo archivo");
        showFileName();
    }

    private void open()
    {
        String content = files.open();

        if (content != null)
        {
            view.getCodeArea().replaceText(content);
            showFileName();
            compile();
        }
    }

    private void save()
    {
        Path saved = files.save(view.getCodeArea().getText());

        if (saved != null)
        {
            showFileName();
            view.setStatus("Guardado en " + saved.toAbsolutePath());
        }
    }

    private void exportTranslation()
    {
        if (lastResult == null || !lastResult.isValid())
        {
            return;
        }

        Path saved = files.saveTranslation(view.getTranslationText());   // lo que el usuario ve

        if (saved != null)
        {
            view.setStatus("Traduccion descargada en " + saved.toAbsolutePath());
        }
    }

    private void showFileName()
    {
        String name = files.getCurrentFileName();

        view.setFileName(name);
        stage.setTitle(Constants.APP_TITLE + "  -  " + name);
    }
}
