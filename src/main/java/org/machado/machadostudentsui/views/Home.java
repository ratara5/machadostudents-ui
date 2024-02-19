package org.machado.machadostudentsui.views;


import com.itextpdf.kernel.colors.DeviceRgb;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.Rol;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.MachadostudentsFxApplication;
import org.machado.machadostudentsui.utils.Menu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.util.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static javafx.collections.FXCollections.observableArrayList;


@Controller
public class Home extends AbstractController { //implements Consumer<List<Student>>
    /*@FXML
    private Label countStudentsLabel;
    @FXML
    private Label countStudentsAssignmentsLabel;*/
    @FXML
    private VBox vBoxTest;


    @Autowired
    private WebClientMachado webClientMachado;

    @FXML
    public void initialize(){
        //webClientMachado.studentsAll().subscribe(this); //Implement Consumer
        //int totalStudents = students.size();
        //countStudentsLabel.setText(String.valueOf(totalStudents));

        List<Student> students = webClientMachado.studentsAll().block();
        int totalStudents = students.size();

        List<Rol> listRol = students.stream()
                .map(Student::getRol)
                .collect(Collectors.toList());
        List<String> listRolName = listRol.stream()
                .map(Rol::getName)
                .collect(Collectors.toList());
        List<String> listDistinctRolName = listRolName.stream()
                .distinct()
                .collect(Collectors.toList());

        HashMap<String, Integer> rolesMap = new LinkedHashMap<>();
        for (String rolName: listDistinctRolName) {
            Integer counted = listRolName.stream()
                    .filter(x-> Objects.equals(x, rolName))
                    .collect(Collectors.toList())
                    .size();
            rolesMap.put(rolName, counted);
        }

        ObservableList<PieChart.Data> chartList = observableArrayList();

        for (String rolName : rolesMap.keySet()){
            PieChart.Data pieChartData = new PieChart.Data(rolName, rolesMap.get(rolName));
            chartList.add(pieChartData);
        }
        final PieChart chart = new PieChart(chartList);

        List<StudentsAssignment> studentsAssignments = webClientMachado.studentsAssignmentAll().block();
        int totalStudentsAssignments = studentsAssignments.size();
        //countStudentsAssignmentsLabel.setText(String.valueOf(totalStudentsAssignments));

        String[] names = {"students", "students assignments"};
        int[] counts = {totalStudents, totalStudentsAssignments};

        HashMap<String, Integer> hashMap = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            hashMap.put(names[i], Integer.valueOf(counts[i]));
        }

        /*String cssVBox = "-fx-border-color: red;\n" +
                "-fx-border-insets: 5;\n" +
                "-fx-border-width: 3;\n" +
                "-fx-border-style: dashed;\n";*/


        String cssVBox = "-fx-padding: 20 20 20 20;" +
                "-fx-border-color: primary; " +
                "-fx-border-width: 2; " +
                "-fx-border-insets: 2; " +
                "-fx-spacing: 10;";

        String cssButton = "-fx-background-color: transparent; " +
                "-fx-text-fill: primary; " +
                "-fx-border-style: solid; " +
                "-fx-border-color: secondary;";

        String cssButtonHover = "-fx-background-color : primary; " +
                "-fx-text-fill: secondary-light;";

        GridPane gridPane = new GridPane();
        vBoxTest.getChildren().add(gridPane);

        /*for (int i = 0; i < 2; i++) {

            VBox vbox = new VBox();
            vbox.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            vbox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));

            Label labelTitle = new Label();
            labelTitle.setText("Total " + names[i]);

            Label labelContent = new Label();
            labelContent.setText(String.valueOf(counts[i]));

            vbox.getChildren().addAll(labelTitle, labelContent);

            gridPane.add(vbox, 0, i+1);

        }*/

        int i=0;
        for (String name: hashMap.keySet()) {

            VBox vbox = new VBox();
            //vbox.setBackground(new Background(new BackgroundFill(Color.BLUE, CornerRadii.EMPTY, Insets.EMPTY)));
            //vbox.setBorder(new Border(new BorderStroke(Color.GREEN, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(3))));

            Label labelTitle = new Label();
            labelTitle.setText(name.toUpperCase());

            Label labelContent = new Label();
            labelContent.setText(String.valueOf(hashMap.get(name)));

            Button button = new Button();
            button.setText(name.toUpperCase());
            button.setStyle(cssButton);
            //if(button.isHover()){button.setStyle(cssButtonHover);}
            button.setOnMouseEntered(e->{button.setStyle(cssButtonHover);});
            button.setOnMouseExited(e->{button.setStyle(cssButton);});
            button.setOnAction(e-> {
                System.out.println("Click en " + button.getText());
                try {
                    button.getScene().getWindow().hide();
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("MainFrame.fxml")); //button.getText()
                    loader.load();
                    MainFrame mainFrame = loader.getController();
                    if(Objects.equals(button.getText(), "STUDENTS")) {
                        mainFrame.loadView(Menu.Student);
                    } else if (Objects.equals(button.getText(), "STUDENTS ASSIGNMENTS")) {
                        mainFrame.loadView(Menu.Assignment);
                    }
                    Parent p = loader.getRoot();
                    Stage s = new Stage();
                    s.setScene(new Scene(p));
                    s.show();
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            });

            vbox.getChildren().addAll(labelTitle, labelContent, button); //chart
            vbox.setStyle(cssVBox);
            if(name.equals("students")){
                vbox.getChildren().add(chart);
            }

            gridPane.add(vbox, i+0, 0);
            i++;
        }

    }

    /*@Override
    public void accept(List<Student> students) {

    }*/

    @FXML
    public void goToStudents() {

    }

    @FXML
    public void goToAssignments() {

    }
}
