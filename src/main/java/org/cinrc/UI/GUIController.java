package org.cinrc.UI;
import com.gluonhq.charm.glisten.control.TextField;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.cinrc.CCSInteractionHandler;
import org.cinrc.IRDC;
import org.cinrc.parser.CCSParser;
import org.cinrc.parser.CCSParserException;
import org.cinrc.parser.LTTNode;
import org.cinrc.process.ProcessTemplate;
import org.cinrc.process.nodes.Label;
import org.cinrc.process.process.Process;
import org.cinrc.util.RCCSFlag;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import java.util.ResourceBundle;

public class GUIController implements Initializable {

    private Stage stage;
    private Scene scene;
    private Parent root;

    @FXML
    public TextArea outputField;
    @FXML
    public TextField inputBox;

    @FXML
    ChoiceBox<String> myChoiceBox;
    @FXML
    Button genRandomProcess;

    private String[] choices = {"Walkthrough", "Enumerate", "Equivalence"}; // values for dropdown box
                                                                            // populated in initailize



    public void genRandom() {
        String action = myChoiceBox.getValue(); // generates different equations based on dropdown selected
        if(action.equals("Enumerate") || action.equals("Walkthrough")){
            genRandomEnum();
        }
        if(action.equals("Equivalence")){
            genRandomEquivalence();
        }
    }

    private void genRandomEquivalence() {
        outputField.setText("");
        String[] eProcesses = {"a.a,b|c,c.d,a.a", "a.b.c|d,b+c", "a.a,a.a", "a.b + c, d.e|a"}; // list of equivalence
                                                                                                // proccess examples
        inputBox.setFloatText("");
        int random = (int) Math.floor(Math.random() * eProcesses.length);
        inputBox.setText(eProcesses[random]);
    }

    public void genRandomEnum() {
        outputField.setText("");
        // processes taken from https://github.com/CinRC/IRDC-CCSK/blob/dev/docs/example_processes.md
        String[] rProcesses = {"((a)|('a))+((a)|('a))", "(a|'a)\\{a}", "a.b.c.d | 'a.'b.'c.'d", // generic processes
                "(a.b | 'b.'a)\\{a, b}", "(a)\\{'a}", // stuck processes
                "a.a.(b+c)", "a.a.b + a.a.c",  // bisiumlation
                "a.P", "a.b.P", "a.(b.X|Y)", "a.P\\{b}", "a.P|b.Q", "a.P + b.Q", "a.P|'a.Q"
        }; // examples from LTS/SOS
        inputBox.setFloatText("");
        int randomNum = (int) Math.floor(Math.random() * rProcesses.length);
        inputBox.setText(rProcesses[randomNum]);
    }

    public void evaluate(ActionEvent event) throws IOException { // acts based on dropdown menu input
        String action = myChoiceBox.getValue();
        if(action.equals("Enumerate")){
            enumerate();
        }
        if(action.equals("Walkthrough")){
            walkthroughPage(event);
        }
        if(action.equals("Equivalence")){
            equivalence();
        }
    }

    private void equivalence() {
        try{

            outputField.setText(""); // clears outfield from previous text
            ArrayList<Process> processes = new ArrayList<>();
            String[] formula = inputBox.getText().split(",");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < formula.length; i++) {
                try {
                    Process p = new CCSParser().parseLine(formula[i]);
                    sb.append(String.format("[%d] %s => Parsed Successfully.\n", i, p.represent()));
                    processes.add(p);
                } catch (CCSParserException e) {
                    outputField.setText(formula[i] + " is not properly formatted! Please check formatting guidelines.");
                }
            }
            sb.append("\n\n");
            List<Pair<String, String>> simulates = new ArrayList<>();
            for (Process p : processes) {
                for (Process p2 : processes) {
                    if (p == p2) {
                        continue;
                    }
                    if (p2.simulates(p)) {
                        simulates.add(new Pair(p2.represent(), p.represent()));
                    }
                }
            }
            sb.append("Simulations and Bisimulations: \n ------------\n");
            for (Pair<String, String> e : simulates) { //Print simulations
                sb.append(String.format("%s %s %s\n", e.getValue(), "≲", e.getKey()));

                for (Pair<String, String> pair : simulates) {
                    if (pair == e) {
                        continue;
                    }

                    if (pair.getKey().equals(e.getValue()) && e.getKey().equals(pair.getValue())) {
                        sb.append(String.format("%s %s %s\n", pair.getKey(), "≈", pair.getValue()));
                    }
                }
            }
            if(outputField.getText().length() == 0) {
                outputField.setText(sb.toString());
            }
        }// same function in IRDC apart from a few changes
        catch(Exception e){
            outputField.setText(e.toString());
        }
    }

    public void enumerate(){
        String process = inputBox.getText();
        try{
            IRDC.config.add(RCCSFlag.ENUMERATE); // adds enumerate flag to allow enumeration of process
            if(process.startsWith("\"")){
                process = process.substring(1);
            }
            if(process.endsWith("\"")){
                process = process.substring(0, process.length() - 1);
            }
            org.cinrc.process.process.Process p = new CCSParser().parseLine(process); // parses through user input
            LTTNode node = new LTTNode(p);
            node.enumerate(true); // enumerates through process
            outputField.setText(String.valueOf(node)); // outputs process
            inputBox.setText("");
            IRDC.config.remove(RCCSFlag.ENUMERATE);
        }
        catch(Exception e){
            IRDC.config.remove(RCCSFlag.ENUMERATE);
            outputField.setText(process + " is not properly formatted! Please check formatting guidelines.");
        }
    }
    public void walkthroughPage(ActionEvent event) throws IOException{
        if(inputBox != null){
            WalkthroughController.walkthroughInput = inputBox.getText();
        }
        Parent root = FXMLLoader.load(getClass().getResource("/ui/walkthroughPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene (root);
        stage.setScene(scene);
        stage.setResizable(false);

        stage.show();

    }
    //switches the view from the main page to the main information page.
    public void infoPage(ActionEvent event) throws IOException{
        Parent root = FXMLLoader.load(getClass().getResource("/ui/infoPage.fxml"));
        stage = (Stage)((Node)event.getSource()).getScene().getWindow();
        scene = new Scene (root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }
    public void openLink(ActionEvent event) throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://github.com/CinRC/IRDC-CCSK"));
    }
    public void openReadme(ActionEvent event) throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://github.com/CinRC/IRDC-CCSK/blob/master/README.md"));
    };

    public void openProcessExamples(ActionEvent event) throws URISyntaxException, IOException {
        Desktop.getDesktop().browse(new URI("https://github.com/CinRC/IRDC-CCSK/blob/dev/docs/example_processes.md"));
    };

    public void onChoiceBoxChange() {
        if(myChoiceBox.getValue().equals("Equivalence")){
            outputField.setText("Enter processes separated by commas to view their equivalences.");
            if(inputBox.getText().length() == 0){
                inputBox.setText("");
                inputBox.setFloatText("Processes");
            }
        }
        else if(myChoiceBox.getValue().equals("Enumerate")){
            outputField.setText("Enter a process to view it broken down in a tree format.");
            if(inputBox.getText().length() == 0){
                inputBox.setText("");
                inputBox.setFloatText("Process");
            }
        }
        else{
            if(inputBox.getText().length() == 0){
                inputBox.setText("");
                inputBox.setFloatText("Process");
            }
        }
    }
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        myChoiceBox.getItems().addAll(choices);
        myChoiceBox.setValue("Walkthrough");

        myChoiceBox.valueProperty().addListener((observableValue, s, t1) ->
                onChoiceBoxChange()); // resets input box when choice is chosen. e.g. walkthrough > equivalence
    }
}
