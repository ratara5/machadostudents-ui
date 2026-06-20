package org.machado.machadostudentsui.views.popups;


import org.machado.machadostudentsclient.PosException;
import org.machado.machadostudentsclient.entity.Role;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class RoleEdit {

    @FXML
    private Label title;
    @FXML
    private Label message;
    @FXML
    private TextField name;

    private Consumer<Role> saveHandler;
    private Role role;

    public static void showView(Consumer<Role> saveHandler) {
        showView(null, saveHandler);
    }

    public static void showView(Role role, Consumer<Role> saveHandler) {
        try {

            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(RoleEdit.class.getResource("RoleEdit.fxml"));
            stage.setScene(new Scene(loader.load()));

            RoleEdit edit = loader.getController();
            edit.init(role, saveHandler);

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(Role role, Consumer<Role> saveHandler) {

        this.saveHandler = saveHandler;

        if(null == role) {
            this.role = new Role();
            this.title.setText("Add New Category");
        } else {
            this.role = role;
            this.title.setText("Edit Category");
            this.name.setText(role.getName());
        }

    }

    @FXML
    private void close() {
        name.getScene().getWindow().hide();
    }

    @FXML
    private void save() {

        try {
            role.setName(name.getText());
            saveHandler.accept(role);
            close();
        } catch (PosException e) {
            message.setText(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}