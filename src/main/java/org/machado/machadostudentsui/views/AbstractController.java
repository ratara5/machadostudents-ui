package org.machado.machadostudentsui.views;


import org.machado.machadostudentsui.utils.Menu;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public abstract class AbstractController {

    @FXML
    private Label title;

    public void setTitle(Menu menu) {
        this.title.setText(menu.getTitle());
    }

}
