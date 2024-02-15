package org.machado.machadostudentsui.views.popups;


import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.fxml.FXML;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.machado.machadostudentsclient.entity.Contact;
import org.machado.machadostudentsclient.entity.Rol;
import org.machado.machadostudentsclient.entity.Student;
import org.machado.machadostudentsclient.entity.StudentsAssignment;
import org.machado.machadostudentsclient.WebClientMachado;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
    private TextField genreField;
    @FXML
    private TextField phoneNumberField;
    @FXML
    private TextField addressField;
    @FXML
    private TextField emailField;
    @FXML
    private ComboBox<Contact> contact;

    private Student student;
    private WebClientMachado webClientMachado;
    private Consumer<Student> saveHandler;

    public static void addNew(Consumer<Student> saveHandler, Supplier<List<Rol>> supplier, List<Contact> contacts) {
        edit(null, saveHandler, supplier, contacts);
    }

    public static void edit(Student student, Consumer<Student> saveHandler, Supplier<List<Rol>> supplier, List<Contact> contacts) {

        try {
            Stage stage = new Stage(StageStyle.UNDECORATED);
            FXMLLoader loader = new FXMLLoader(StudentEdit.class.getResource("StudentEdit.fxml"));
            stage.setScene(new Scene(loader.load()));
            stage.initModality(Modality.APPLICATION_MODAL);

            StudentEdit controller = loader.getController();
            controller.init(student, saveHandler, supplier, contacts);

            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @FXML
    private void save() {

        student.setRolId(rol.getValue().getRolId());
        student.setName(nameField.getText());
        student.setLastName(lastNameField.getText());
        student.setGenre(genreField.getText());
        student.setPhoneNumber(phoneNumberField.getText());
        student.setAddress(addressField.getText());
        student.setEmail(emailField.getText());
        student.setContactId(contact.getValue().getContactId());

        List<StudentsAssignment> studentsAssignments = new ArrayList<>();
        student.setAssignments(studentsAssignments);

        saveHandler.accept(student);
        close();

    }

    private void init(Student student,
                      Consumer<Student> saveHandler,
                      Supplier<List<Rol>> supplier,
                      List<Contact> contacts){

        this.student = student;
        this.saveHandler = saveHandler;
        rol.getItems().addAll(supplier.get());
        contact.getItems().addAll(contacts);

        if(null == student) {
            title.setText("Add New Student");
            this.student = new Student();

        } else {
            title.setText("Edit Student");
        }
        rol.setValue(this.student.getRol());
        nameField.setText(this.student.getName());
        lastNameField.setText(this.student.getLastName());
        phoneNumberField.setText(this.student.getPhoneNumber());
        addressField.setText(this.student.getAddress());
        emailField.setText(this.student.getEmail());
        contact.setValue(this.student.getContact());

    }

    @FXML
    private void close() {
        title.getScene().getWindow().hide();
    }

}