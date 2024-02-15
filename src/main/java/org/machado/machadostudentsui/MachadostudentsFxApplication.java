package org.machado.machadostudentsui;


import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.machado.machadostudentsui.views.MainFrame;

import java.awt.*;

public class MachadostudentsFxApplication extends Application {

    private static ConfigurableApplicationContext applicationContext;

    public static ConfigurableApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void init() throws Exception{
        applicationContext = new SpringApplicationBuilder(MachadostudentsUiApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {

        //Set icon on the application bar
        Image appIcon = new Image("/images/oratory.png");
        stage.getIcons().add(appIcon);

        //Set icon on the taskbar/dock
        if (Taskbar.isTaskbarSupported()) {
            var taskbar = Taskbar.getTaskbar();

            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
                var dockIcon = defaultToolkit.getImage(getClass().getResource("/images/oratory.png"));
                taskbar.setIconImage(dockIcon);
            }

        }

        /*//String musicFile = "/sounds/clapping.mp3";
        String musicFile = "/home/ratara5/Documents/ideaProjects/machadostudents-ui/src/main/resources/sounds/clapping.mp3";

        Media sound = new Media(new File(musicFile).toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        mediaPlayer.play();*/

        MainFrame.show();

    }

    @Override
    public void stop() throws Exception{
        applicationContext.close();
        Platform.exit();
    }

}