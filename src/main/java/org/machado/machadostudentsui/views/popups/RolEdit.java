package org.machado.machadostudentsui.views.popups;


import org.machado.machadostudentsclient.PosException;
import org.machado.machadostudentsclient.entity.Rol;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class RolEdit {

    @FXML
    private Label title;
    @FXML
    private Label message;
    @FXML
    private TextField name;

    private Consumer<Rol> saveHandler;
    private Rol rol;

    public static void showView(Consumer<Rol> saveHandler) {
        showView(null, saveHandler);
    }

    public static void showView(Rol rol, Consumer<Rol> saveHandler) {
        try {

            Stage stage = new Stage(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);

            FXMLLoader loader = new FXMLLoader(RolEdit.class.getResource("RolEdit.fxml"));
            stage.setScene(new Scene(loader.load()));

            RolEdit edit = loader.getController();
            edit.init(rol, saveHandler);

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void init(Rol rol, Consumer<Rol> saveHandler) {

        this.saveHandler = saveHandler;

        if(null == rol) {
            this.rol = new Rol();
            this.title.setText("Add New Category");
        } else {
            this.rol = rol;
            this.title.setText("Edit Category");
            this.name.setText(rol.getName());
        }

    }

    @FXML
    private void close() {
        name.getScene().getWindow().hide();
    }

    @FXML
    private void save() {

        try {
            rol.setName(name.getText());
            saveHandler.accept(rol);
            close();
        } catch (PosException e) {
            message.setText(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}