package org.machado.machadostudentsui.views;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.function.Consumer;

@Controller
public class Home extends AbstractController { //implements Consumer<List<Student>>
    @FXML
    private Label countStudentsLabel;
    @FXML
    private Label countStudentsAssignmentsLabel;

    @Autowired
    private WebClientMachado webClientMachado;

    @FXML
    public void initialize(){
        //webClientMachado.studentsAll().subscribe(this); //Implement Consumer

        List<Student> students = webClientMachado.studentsAll().block();
        int totalStudents = students.size();
        countStudentsLabel.setText(String.valueOf(totalStudents));

        List<StudentsAssignment> studentsAssignments = webClientMachado.studentsAssignmentAll().block();
        int totalStudentsAssignments = studentsAssignments.size();
        countStudentsAssignmentsLabel.setText(String.valueOf(totalStudentsAssignments));

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
