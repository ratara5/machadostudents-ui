module machadostudentsui {
    requires machadostudentsclient;// Requerimiento
    requires spring.boot;
    requires spring.boot.starter;
    requires javafx.graphics;
    requires javafx.fxml;
    requires javafx.media;
    //requires wiremock.core;  // WireMock con módulo adecuado en el classpath
    requires kernel;
    requires layout;
    requires forms;
    requires javafx.controls;
    requires spring.beans;
    requires spring.context;
    requires java.datatransfer;
    requires java.desktop;
    requires spring.boot.autoconfigure;
    requires reactor.core;

    exports org.machado.machadostudentsui;
}
