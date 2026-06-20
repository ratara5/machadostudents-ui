package org.machado.machadostudentsui.views;


import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.shape.SVGPath;
import org.machado.machadostudentsclient.entity.Role;
import org.machado.machadostudentsclient.WebClientMachado;
import org.machado.machadostudentsui.views.popups.RoleEdit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.function.Consumer;

@Controller
public class Roles extends AbstractController {

    @FXML
    private TilePane container;

    @Autowired
    private WebClientMachado webClientMachado;

    @FXML
    private void initialize() {
        reload();
    }

    @FXML
    private void addNew() {
        RoleEdit.showView(this::save);
    }

    private void save(Role role) {
        //webClientMachadostudentsClient.addRole(role); //TODO: Create Role methods in client
        reload();
    }

    private void reload() {
        container.getChildren().clear();
        //webClientMachadostudentsClient.findAll().stream().forEach(role -> role.getChildren().add(new RoleItem(role, this::save))); //TODO: Create Role methods in client
    }

    private class RoleItem extends HBox {

        public RoleItem(Role role, Consumer<Role> consumer) {

            SVGPath icon = new SVGPath();
            icon.setContent("M7 7c0-1.109-0.891-2-2-2s-2 0.891-2 2 0.891 2 2 2 2-0.891 2-2zM23.672 16c0 0.531-0.219 1.047-0.578 1.406l-7.672 7.688c-0.375 0.359-0.891 0.578-1.422 0.578s-1.047-0.219-1.406-0.578l-11.172-11.188c-0.797-0.781-1.422-2.297-1.422-3.406v-6.5c0-1.094 0.906-2 2-2h6.5c1.109 0 2.625 0.625 3.422 1.422l11.172 11.156c0.359 0.375 0.578 0.891 0.578 1.422z");
            Label name = new Label();
            name.setText(role.getName());

            getChildren().addAll(icon, name);
            getStyleClass().add("role-item");

            setOnMouseClicked(event -> {

                if(event.getClickCount() == 2){
                    RoleEdit.showView(role, consumer);
                }
            });
        }

    }

}