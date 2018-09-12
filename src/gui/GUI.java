package gui;

import assembulator.Assembler;
import assembulator.Assembulator;
import gui.factories.ButtonFactory;
import gui.factories.MenuBarFactory;
import gui.factories.MenuItemFactory;
import gui.factories.TextFieldFactory;
import instructions.BadInstructionException;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class GUI implements Executable{

    public static final int PREF_SIZE = 500;
    public static final double TITLE_WIDTH = 0.8 * PREF_SIZE;
    public static final double PREF_LOG_HEIGHT = 0.4 * PREF_SIZE;
    public static final double PREF_LOG_WIDTH = 0.9 * PREF_SIZE;
    public static final int PADDING = 10;
    public static final String LAUNCH = "launch";
    public static final String INPUT_BUTTON = "inputButton";
    public static final String OUTPUT_BUTTON = "outputButton";
    public static final String OVERWRITE = "overwrite";
    public static final String PAD_ALL = "all";
    public static final String PAD_NONE = "none";
    public static final String SEARCH_SUBDIRS = "subdirs";
    public static final String CREATE_OUT_DIR = "outdir";
    public static final String CLC_EACH = "clcEach";
    public static final String CLC_NOW = "CLC_NOW";
    public static final String FALSE = "false";
    public static final String TRUE = "true";

    private TextField input ;
    private TextField output;
    private MenuBar topBar;
    private Map<String, Button> buttons = new HashMap<>();
    private Map<String, CheckMenuItem> listenerMap = new HashMap<>();
    private BorderPane bp = new BorderPane();
    private Assembler assembler = new Assembulator();
    private TextArea log = new TextArea();
    private ImageView title = new ImageView(new Image(this.getClass().getClassLoader().getResourceAsStream("350_Assembler.png")));
    private Map<String, EventHandler<ActionEvent>> handlerMap = new HashMap<>();

    public GUI(Stage s){
        initElements();
        Scene scene = new Scene(packageElements());
        s.setScene(scene);
        s.show();
    }

    private BorderPane packageElements(){
        VBox center = packageCenter();

        BorderPane logoPane = new BorderPane();
        logoPane.setCenter(title);
        title.setPreserveRatio(true);
        title.setFitWidth(TITLE_WIDTH);

        configBP(center, logoPane);

        BorderPane barPane = new BorderPane();
        barPane.setTop(topBar);

        BorderPane masterPane = new BorderPane();
        masterPane.setTop(barPane);
        masterPane.setBottom(bp);

        return masterPane;
    }

    private void configBP(VBox center, BorderPane logoPane) {
        bp.setTop(logoPane);
        bp.setCenter(center);
        bp.setBottom(log);
        bp.setPrefHeight(PREF_SIZE);
        bp.setPrefWidth(PREF_SIZE);
        bp.setPadding(new Insets(PADDING));
    }

    private VBox packageCenter() {
        VBox center = new VBox();
        BorderPane buttonBorderPane = new BorderPane();
        buttonBorderPane.setRight(buttons.get(LAUNCH));
        center.getChildren().add(buttonBorderPane);
        VBox in = packageTextAndButton(input, buttons.get(INPUT_BUTTON));
        VBox out = packageTextAndButton(output, buttons.get(OUTPUT_BUTTON));
        center.getChildren().add(in);
        center.getChildren().add(out);
        return center;
    }

    private void initElements(){
        buildTextFields();
        buildMenuBar();
        buildButtons();
        buildLog();
    }

    private void buildLog() {
        log.setEditable(false);
        log.setPrefSize(PREF_LOG_WIDTH, PREF_LOG_HEIGHT);
    }

    private VBox packageTextAndButton(TextField tf, Button b){
        VBox ret = new VBox();
        ret.setPadding(new Insets(PADDING));
        ret.getChildren().add(tf);
        ret.getChildren().add(b);
        return ret;
    }

    private void buildButtons() {
        Map<String, String> labels = Map.of(LAUNCH, "Launch", INPUT_BUTTON,"...", OUTPUT_BUTTON,"...");
        Map<String, EventHandler<ActionEvent>> actions = Map.of(LAUNCH, (e)->go(),
                INPUT_BUTTON, e->assignField(input, DialogFactory.fileLoadChooser("MIPS code (*.s)", "*.s",
                        "Assembly code (*.asm)", "*.asm")),
                OUTPUT_BUTTON, e->assignField(output, DialogFactory.fileSaveChooser()));
        for(String button : labels.keySet())
            buttons.put(button, ButtonFactory.getInstance(labels.get(button), actions.get(button)));
    }

    private void buildMenuBar() {
        List<String> structure = List.of(
                MenuBarFactory.MENU, "File",
                    MenuBarFactory.MENU, "Protection Level",
                        MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "Pad all", PAD_ALL, FALSE,
                        MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "No padding", PAD_NONE, TRUE,
                    MenuBarFactory.BREAK,
                    MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "Overwrite files?", OVERWRITE, TRUE,
                    MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "Search subdirs?", SEARCH_SUBDIRS, FALSE,
                    MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "Add output dir?", CREATE_OUT_DIR, TRUE,
                MenuBarFactory.BREAK,
                MenuBarFactory.MENU, "Edit",
                    MenuBarFactory.MENU_ITEM, MenuItemFactory.DEFAULT_MENU, "Clear log", CLC_NOW,
                    MenuBarFactory.MENU_ITEM, MenuItemFactory.CHECK_MENU, "Clear log before runs", CLC_EACH, FALSE,
                MenuBarFactory.BREAK
        );

        //Sadly can't be done automatically -- each menu may do something different
        handlerMap.put(CLC_NOW, e->log.clear());

        topBar = MenuBarFactory.getInstance(structure, listenerMap, handlerMap);
        makeExclusive(listenerMap.get(PAD_ALL), listenerMap.get(PAD_NONE));
    }


    private void makeExclusive(CheckMenuItem ml1, CheckMenuItem ml2){
        ml1.setOnAction(e->ml2.setSelected(!ml1.isSelected()));
        ml2.setOnAction(e->ml1.setSelected(!ml2.isSelected()));
    }


    private void buildTextFields() {
        input = TextFieldFactory.getInstance("Input file/directory");
        output = TextFieldFactory.getInstance("Output directory");
    }

    private void assignField(TextField tf, File f){
        if(f == null) return;
        try{
            String path = f.getCanonicalPath();
            tf.setText(path);
        } catch (IOException e){
            DialogFactory.showError(e);
        }
    }

    @Override
    public void go(){ // TODO break up into smaller methods
        if(listenerMap.get("clcEach").isSelected()) log.clear(); // clear before each run == true
        String inString = input.getText();
        String outString = output.getText();
        if(inString.isEmpty() || outString.isEmpty()){
            DialogFactory.showError("Must specify input and output locations.");
            return;
        }
        File in = new File(inString);
        File out = (listenerMap.get("outdir").isSelected()) ? new File(String.join(File.separator,
                outString, "mif_outputs")) : new File(outString);
        if(!out.exists()) out.mkdirs();
        try {
            if (in.isDirectory()) {
                encodeAllFiles(in, out);
            } else {
                writeFile(in, getOutputFileFromDirectory(out, in.getName()));
            }
        } catch(IOException e){
            DialogFactory.showError(e);
            log.appendText(String.format("!!FAILURE!! -- %s\n", e.getMessage()));
        }
    }

    private void encodeAllFiles(File in, File out) throws IOException, BadInstructionException{
        Queue<File> files = new LinkedList<>(Arrays.asList(in.listFiles()));
        while(!files.isEmpty()) {
            File file = files.remove();
            if(listenerMap.get("subdirs").isSelected() && file.isDirectory()){
                files.addAll(Arrays.asList(file.listFiles()));
            } else if(!file.isDirectory()){
                String name = file.getName(); // TODO extract method -- also replace line 168
                File outputFile = getOutputFileFromDirectory(out, name); // TODO check for valid extension -- .s, .asm
                writeFile(file, outputFile);
            }
        }
    }

    private File getOutputFileFromDirectory(File out, String name) throws IOException {
        StringBuilder sb = new StringBuilder(name);
        sb.delete(sb.lastIndexOf("."), sb.length());
        sb.append(".mif");
        return new File(String.join(File.separator, out.getCanonicalPath(), sb.toString()));
    }

    private void writeFile(File fin, File fout) throws IOException {
        log.appendText(String.format("Attempting to write %s to %s\n", fin.getName(), fout.getName()));
        if(fout.exists() && !listenerMap.get("overwrite").isSelected()){
            log.appendText(String.format("File %s already exists. To set overwrite, update menu setting in File->Overwrite Files\n", fout.getName()));
            return;
        }
        FileInputStream fis = new FileInputStream(fin);
        FileOutputStream fos = new FileOutputStream(fout);
        try {
            assembler.writeTo(fis, fos, listenerMap.get("all").isSelected());
            fis.close();
            fos.close(); // TODO extract method
            log.appendText("Success\n");
        } catch (BadInstructionException e) {
            DialogFactory.showError(e);
            fos.close();
            fis.close(); // TODO extract method
            fout.delete();
            log.appendText(String.format("!!FAILURE!! -- %s", e.getMessage()));
        }
    }
}
