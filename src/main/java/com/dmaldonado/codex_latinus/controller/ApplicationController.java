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
 * EL PUENTE (MVC). Unico punto donde la vista y el modelo se encuentran: la
 * vista no sabe de ANTLR y LatinCompiler no sabe de JavaFX.
 *
 * No hay un CompilerController envolviendo a LatinCompiler: el modelo ya corre
 * el pipeline y ya devuelve un CompilationResult agnostico de la interfaz.
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

        // Doble clic en un error lleva el cursor hasta el.
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

        // El enunciado prohibe graficar y traducir un programa con errores: se
        // limpian ambos paneles en vez de dejar un resultado viejo en pantalla.
        view.showAst(lastResult.isValid() ? AstTreeBuilder.build(lastResult.ast()) : null);
        view.showTranslation(lastResult.isValid() ? translate() : "");
        view.getExportButton().setDisable(!lastResult.isValid());

        // La pila se muestra compile o no: el archivo invalido es justo aquel
        // cuyos pasos ERROR hay que leer.
        view.getProcessStackPanel().load(lastResult.steps());

        view.setStatus(summaryOf(lastResult), lastResult.isValid());
        view.showResultTab(lastResult.isValid());
    }

    /** La casilla no recompila: retraduce el AST que ya esta en memoria. */
    private void refreshTranslation()
    {
        if (lastResult != null && lastResult.isValid())
        {
            view.showTranslation(translate());
        }
    }

    private String translate()
    {
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

        // El error es 1-based y el editor 0-based, y hay que acotar: un error al
        // final del archivo puede traer una columna mayor al largo de su linea.
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
