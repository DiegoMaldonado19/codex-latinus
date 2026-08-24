package com.dmaldonado.codex_latinus.view;

import com.dmaldonado.codex_latinus.model.errors.CompilerError;
import com.dmaldonado.codex_latinus.model.symbols.Symbol;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

/**
 * LA VISTA (MVC). Arma la ventana, expone sus controles y pinta un resultado ya
 * calculado. No compila ni lee archivos: ApplicationController es el unico
 * puente al modelo.
 */
public class MainView extends BorderPane {
    private final CodeArea codeArea = HighlightingCodeArea.create();
    private final TableView<CompilerError> errorTable = new TableView<>();
    private final TableView<Symbol> symbolTable = new TableView<>();
    private final TreeView<String> astTree = new TreeView<>();
    private final ProcessStackPanel stackPanel = new ProcessStackPanel();
    private final TextArea translation = new TextArea();
    private final TabPane tabs = new TabPane();

    // Como campos y no como indices: reordenar las pestanas no puede mandar al
    // usuario a la equivocada.
    private final Tab errorTab = new Tab("Errores", errorTable);
    private final Tab translationTab = new Tab("PigLatin");

    // Solo queda esta: el enunciado no obliga a traducir el INTERIOR de un
    // textum, asi que ahi si hay una eleccion. Las palabras reservadas se
    // traducen siempre.
    private final CheckBox translateStringsBox = new CheckBox("Traducir contenido de los textos");

    private final Button newButton = new Button("Nuevo");
    private final Button openButton = new Button("Abrir .lat");
    private final Button saveButton = new Button("Guardar");
    private final Button compileButton = new Button("Compilar");
    private final Button exportButton = new Button("Descargar .pig");

    private final Label statusLabel = new Label("Listo");
    private final Label fileLabel = new Label("Sin archivo");

    public MainView() {
        buildErrorTable();
        buildSymbolTable();

        translation.setEditable(false);
        translation.getStyleClass().add("translation-area");
        exportButton.setDisable(true); // nothing valid has been compiled yet

        statusLabel.setPadding(new Insets(6, 12, 6, 12));

        setTop(buildToolBar());
        setCenter(buildCenter());
        setBottom(statusLabel);
    }

    /*
     * ================================================================
     * Construction
     * ================================================================
     */

    private ToolBar buildToolBar() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        compileButton.getStyleClass().add("compile-button");

        return new ToolBar(newButton, openButton, saveButton, new Separator(),
                compileButton, new Separator(), exportButton, spacer, fileLabel);
    }

    private SplitPane buildCenter() {
        VirtualizedScrollPane<CodeArea> editorScroll = new VirtualizedScrollPane<>(codeArea);

        translationTab.setContent(buildTranslationTab());

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(errorTab,
                new Tab("AST", astTree),
                new Tab("Tabla de simbolos", symbolTable),
                new Tab("Pila de procesos", stackPanel),
                translationTab);

        SplitPane split = new SplitPane(editorScroll, tabs);
        split.setDividerPositions(0.45);
        return split;
    }

    private BorderPane buildTranslationTab() {
        // En sincronia con el valor por defecto de PigLatinTranslator.
        translateStringsBox.setSelected(false);

        HBox options = new HBox(16, translateStringsBox);
        options.setPadding(new Insets(8));

        BorderPane panel = new BorderPane(translation);
        panel.setTop(options);
        return panel;
    }

    private void buildErrorTable() {
        errorTable.setPlaceholder(new Label("Sin errores."));
        addColumn(errorTable, "Tipo", 110, CompilerError::getType);
        addColumn(errorTable, "Linea", 70, CompilerError::getLine);
        addColumn(errorTable, "Columna", 80, CompilerError::getColumn);
        addColumn(errorTable, "Lexema", 140, CompilerError::getLexeme);
        addColumn(errorTable, "Descripcion", 460, CompilerError::getDescription);
    }

    private void buildSymbolTable() {
        symbolTable.setPlaceholder(new Label("Sin simbolos: el programa no llego al analisis semantico."));
        addColumn(symbolTable, "Nombre", 160, Symbol::getName);
        addColumn(symbolTable, "Categoria", 100, Symbol::getCategory);
        addColumn(symbolTable, "Tipo", 110, Symbol::getTypeText);
        addColumn(symbolTable, "Ambito", 130, Symbol::getScopeName);
        addColumn(symbolTable, "Linea", 70, Symbol::getLine);
        addColumn(symbolTable, "Columna", 80, Symbol::getColumn);
        addColumn(symbolTable, "Detalle", 300, Symbol::getDetail);
    }

    /**
     * Lambda y no PropertyValueFactory: esa resuelve el getter por reflexion
     * sobre un String y fallaria en ejecucion con una columna vacia. La columna
     * conserva el tipo real del valor, asi el orden por Linea es numerico.
     */
    private static <S, T> void addColumn(TableView<S> table, String title, double width,
            Function<S, T> value) {
        TableColumn<S, T> column = new TableColumn<>(title);

        column.setPrefWidth(width);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        table.getColumns().add(column);
    }

    /*
     * ================================================================
     * Painting a result
     * ================================================================
     */

    public void showErrors(List<CompilerError> errors) {
        errorTable.setItems(FXCollections.observableArrayList(errors));
    }

    public void showSymbols(List<Symbol> symbols) {
        symbolTable.setItems(FXCollections.observableArrayList(symbols));
    }

    /** Raiz null limpia la pestana: un programa con errores no se grafica. */
    public void showAst(TreeItem<String> root) {
        astTree.setRoot(root);
    }

    public void showTranslation(String pigLatinCode) {
        translation.setText(pigLatinCode);
    }

    public void setStatus(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("status-ok", "status-error");
    }

    public void setStatus(String message, boolean successful) {
        setStatus(message);
        statusLabel.getStyleClass().add(successful ? "status-ok" : "status-error");
    }

    public void setFileName(String name) {
        fileLabel.setText(name);
    }

    /** Adelanta lo que el usuario necesita: los errores, o el resultado. */
    public void showResultTab(boolean successful) {
        tabs.getSelectionModel().select(successful ? translationTab : errorTab);
    }

    /*
     * ================================================================
     * Accessors for the controller
     * ================================================================
     */

    public String getTranslationText() {
        return translation.getText();
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public TableView<CompilerError> getErrorTable() {
        return errorTable;
    }

    public ProcessStackPanel getProcessStackPanel() {
        return stackPanel;
    }

    public CheckBox getTranslateStringsBox() {
        return translateStringsBox;
    }

    public Button getNewButton() {
        return newButton;
    }

    public Button getOpenButton() {
        return openButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public Button getCompileButton() {
        return compileButton;
    }

    public Button getExportButton() {
        return exportButton;
    }
}
