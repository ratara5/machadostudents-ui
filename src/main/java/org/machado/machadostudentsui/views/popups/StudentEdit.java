package org.machado.machadostudentsui.views.popups;


import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.machado.machadostudentsclient.entity.*;
import org.machado.machadostudentsui.views.Students;
import org.machado.machadostudentsui.views.common.Dialog;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class StudentEdit {

    @FXML
    private ComboBox<Rol> rol;
    @FXML
    private Label title;
    @FXML
    private Label message;
    @FXML
    private TextField nameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private ToggleGroup genderToggleGroup;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<Contact> contact;
    private FilteredList<Assignment> filteredAssignments;
    @FXML
    private TableView<Assignment> studentAssignmentsTable;


    private Student student;
    private Consumer<Student> saveHandler;
    private Students studentController;
    private FXMLLoader studentLoader;

    public static void addNew(Consumer<Student> saveHandler,
                              Supplier<List<Rol>> supplier,
                              List<Contact> contacts/*,
                              List<StudentsAssignment> studentsAssignments,
                              List<Assignment> assignments,
                              Students studentController,
                              FXMLLoader studentLoader*/) {
        edit(null,
                saveHandler,
                supplier,
                contacts,
                null,
                new ArrayList<>(),
                null,
                null);
    }

    public static void edit(Student student,
                            Consumer<Student> saveHandler,
                            Supplier<List<Rol>> supplier,
                            List<Contact> contacts,
                            List<StudentsAssignment> studentsAssignments,
                            List<Assignment> assignments,
                            Students studentController,
                            FXMLLoader studentLoader) {

        try {
            Stage stage = new Stage(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader(StudentEdit.class.getResource("StudentEdit.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);

            StudentEdit controller = loader.getController();
            controller.init(student,
                    saveHandler,
                    supplier,
                    contacts,
                    studentsAssignments,
                    assignments,
                    studentController,
                    studentLoader);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void save() {

        RadioButton selectedRadioButton = (RadioButton) genderToggleGroup.getSelectedToggle();

        Dialog.DialogBuilder.builder()
                .title("Save Changes")
                .message(String.format("Do you want to save changes for Student %s?", nameField.getText()))
                .okActionListener(() -> {

                    student.setRolId(rol.getValue().getRolId());
                    student.setName(nameField.getText());
                    student.setLastName(lastNameField.getText());

                    student.setGenre(selectedRadioButton.getText());

                    student.setPhoneNumber(phoneNumberField.getText());
                    student.setAddress(addressField.getText());
                    student.setEmail(emailField.getText());
                    student.setContactId(contact.getValue().getContactId());

                    saveHandler.accept(student);
                    close();
                })
                .build().show();

    }

    private void init(Student student,
                      Consumer<Student> saveHandler,
                      Supplier<List<Rol>> supplier,
                      List<Contact> contacts,
                      List<StudentsAssignment> studentsAssignments,
                      List<Assignment> assignments,
                      Students studentController,
                      FXMLLoader studentLoader){

        //Initialize Variables
        this.student = student;
        this.saveHandler = saveHandler;
        this.studentController = studentController;
        this.studentLoader = studentLoader;

        //Assign combobox options
        rol.getItems().addAll(supplier.get());
        contact.getItems().addAll(contacts);

        if(null == student) {
            title.setText("Add New Student");
            this.student = new Student();
        } else {
            title.setText("Edit Student");
        }
        /*if (assignments == null) {
            assignments = new ArrayList<>();
            this.student.setAssignments(studentsAssignments);
        }*/

        //Populate fields
        rol.setValue(this.student.getRol());
        nameField.setText(this.student.getName());
        lastNameField.setText(this.student.getLastName());

        if ("H".equals(this.student.getGenre())) {
            genderToggleGroup.selectToggle(genderToggleGroup.getToggles().get(0));
        } else if ("M".equals(this.student.getGenre())) {
            genderToggleGroup.selectToggle(genderToggleGroup.getToggles().get(1));
        }

        phoneNumberField.setText(this.student.getPhoneNumber());
        addressField.setText(this.student.getAddress());
        emailField.setText(this.student.getEmail());
        contact.setValue(this.student.getContact());

        //Initial Predicate
        ObservableList<Assignment> observableAssignmentList = FXCollections.observableList(assignments);
        filteredAssignments = new FilteredList<>(observableAssignmentList);

        Predicate<Assignment> initialFilter = assignment ->
                studentsAssignments.stream()
                        .anyMatch(studentsAssignment -> studentsAssignment.getAssignmentId() == assignment.getAssignmentId());
        filteredAssignments.setPredicate(initialFilter);

        //Populate assignments table
        studentAssignmentsTable.setItems(filteredAssignments);
    }

    @FXML
    private void close() {
        title.getScene().getWindow().hide();
    }

}