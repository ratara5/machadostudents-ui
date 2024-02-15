package org.machado.machadostudentsui;


import org.springframework.boot.autoconfigure.SpringBootApplication;
import javafx.application.Application;

@SpringBootApplication
//@ComponentScan(basePackages = "org.machado.machadostudentsui.controller.AddStudentFormController")
public class MachadostudentsUiApplication {

	public static void main(String[] args) {
		Application.launch(MachadostudentsFxApplication.class, args);
	}

}
