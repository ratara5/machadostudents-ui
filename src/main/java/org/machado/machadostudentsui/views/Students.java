package org.machado.machadostudentsui.views;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsclient.entity.*;
import org.machado.machadostudentsui.views.common.Dialog;
import org.machado.machadostudentsui.views.popups.StudentEdit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Controller
public class Students extends AbstractController implements Consumer<List<Student>>, Supplier<List<Rol>>{ //REMOVE Implements

    @FXML
    private ComboBox<Rol> rol;
    @FXML
    private TextField name;
    @FXML
    private TableView<Student> studentTable;

    @Autowired
    private WebClientMachado webClientMachado;

    @FXML
    public void initialize() {

        FXMLLoader loader = new FXMLLoader(Students.class.getResource("Student.fxml"));
        Students controller = (Students) loader.getController();

        rol.getItems().clear();
        rol.getItems().addAll(this.get());
        studentTable.getItems().clear(); // Implements Consumer
        webClientMachado.studentsAll().subscribe(this); //Implement Consumer


        MenuItem edit = new MenuItem("Edit Student");
        edit.setOnAction(event -> {
            Student student = studentTable.getSelectionModel().getSelectedItem();
            if(null != student) {
                StudentEdit.edit(student,
                        (Consumer<Student>) this::save,
                        (Supplier<List<Rol>>) this::get,
                        (List<Contact>) this.getContacts(), //rolWebClient::rolesAll
                        (List<StudentsAssignment>) this.getStudentsAssignmentByStudent(student.getStudentId()),
                        (List<Assignment>) this.getAssignments(),
                        (Students) controller,
                        (FXMLLoader) loader);
            }
        });

        MenuItem changeState = new MenuItem("Delete Student");
        changeState.setOnAction(event -> {
            Student student = studentTable.getSelectionModel().getSelectedItem();

            Dialog.DialogBuilder.builder()
                    .title("Delete Student")
                    .message(String.format("Do you want to delete student: %s?", student.getName()))
                    .okActionListener(() -> {
                        //webClientMachado.addStudent(student);
                        //search();
                        System.out.println("studentId from UI: " + student.getStudentId() );
                        //webClientMachado.deleteStudent(student.getStudentId()+"");
                        this.delete(student.getStudentId());
                    })
                    .build().show();
        });

        studentTable.setContextMenu(new ContextMenu(edit, changeState));

    }

    private List<StudentsAssignment> getStudentsAssignmentByStudent(int studentId) {
        List<StudentsAssignment> listStudentsAssignment = webClientMachado.assignmentsPerStudent(studentId+"").block();
        return listStudentsAssignment;
    }

    public List<Assignment> getAssignments() {
        List<Assignment> assignments = webClientMachado.assignmentsAll().block();
        return assignments;
    }

    @FXML
    private void search() {
        studentTable.getItems().clear();
        List<Student> studentsByRol = webClientMachado.studentsByRol(rol.getValue().getRolId()+"").block(); //TODO STudent method: search by category
        studentTable.getItems().addAll(studentsByRol);
    }

    @FXML
    private void clear() {
        rol.setValue(null);
        name.clear();
        studentTable.getItems().clear();
    }

    @FXML
    private void addNew() {
        rol.getItems().clear();
        ArrayList<Assignment> assignments= new ArrayList<>();
        ArrayList<StudentsAssignment> studentsAssignments= new ArrayList<>();
        StudentEdit.addNew(this::save,
                this::get,
                this.getContacts()); /*,
                studentsAssignments,
                assignments,
                this,
                null);*/
    }

    private void save(Student student) {
        webClientMachado.addStudent(student);
        initialize();
    }

    private void delete(Integer studentId) {
        webClientMachado.deleteStudent(studentId+"").subscribe();
        initialize();
    }

    @Override
    public void accept(List<Student> students) {
        studentTable.getItems().addAll(students);
    }

    @Override
    public List<Rol> get() {
        List<Rol> rolesAll = webClientMachado.rolesAll().block();
        return rolesAll;
    }

    public List<Contact> getContacts() {
        List<Contact> contactsAll = webClientMachado.contactsAll().block();
        return contactsAll;
    }

}
