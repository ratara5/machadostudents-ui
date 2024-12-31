package org.machado.machadostudentsui.views.popups;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.machado.machadostudentsclient.entity.Assignment;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsui.views.Assignments;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AssignmentEditAux extends AssignmentEditBase {


    @FXML
    private GridPane studentsGridPane;

    public static void edit(Assignment assignment,
                            Consumer<StudentsAssignment> saveHandler,
                            BiConsumer<String, String> deleteHandler,
                            List<StudentsAssignment> listStudentsAssignment,
                            Supplier<List<Student>> supplier,
                            Assignments.RoomProvider roomProvider,
                            Assignments assignmentController,
                            FXMLLoader assignmentLoader) {

        try {
            /*Stage stage = new Stage(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader(AssignmentEditAux.class.getResource("AssignmentEditAux.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);*/

            // SUGERIDO POR GPT
            Stage stage = new Stage(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(AssignmentEdit.class.getResource("AssignmentEdit.fxml"));
            loader.setController(new AssignmentEditAux());
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);


            AssignmentEditAux controller = loader.getController();
            controller.init(assignment,
                    saveHandler,
                    deleteHandler,
                    listStudentsAssignment,
                    supplier,
                    roomProvider,
                    assignmentController,
                    assignmentLoader);
            stage.sizeToScene();
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void processLabels(List<StudentsAssignment> listStudentsAssignment) {
        ////Fill name students labels
        for (Object node : studentsGridPane.getChildren()) {

            if (node instanceof Label) {
                Label label = (Label) node;
                for (int i = 0; i < listStudentsAssignment.size(); i++) {
                    StudentsAssignment studentsAssignment = listStudentsAssignment.get(i);
                    Student student = listStudentsAssignment.get(i).getStudent();
                    if (Objects.equals(studentsAssignment.getRolStudent(), label.getUserData())
                            && Objects.equals(studentsAssignment.getRoom(), "Aux")) {
                        label.setId(studentsAssignment.getStudentId() + "");
                        label.setText(student.getName() + " " + student.getLastName());
                    }
                }
            }
        }
    }

}