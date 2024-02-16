package org.machado.machadostudentsui.utils;


public enum Menu {

    Home("AYFM Dashboard"),
    Assignment("Assignments Management"),
    Rol("Roles Management"),
    Student("Students Management");

    private String title;

    Menu(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String getFxml() {
        return String.format("%s.fxml", name());
    }

}
