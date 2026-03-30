package org.example.frontend;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.control.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;

import javafx.application.Application;
import javafx.scene.layout.GridPane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class RoleSelection extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(30);
        grid.setPadding(new Insets(25));

        Text sceneTitle = new Text("Select your role");
        // sceneTitle.setTextAlignment(TextAlignment.CENTER);
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.BOLD, 30));
        sceneTitle.setFill(Color.WHITE);
        // sceneTitle.setStroke(Color.BLACK);
        grid.add(sceneTitle, 0, 0, 3, 1);
        GridPane.setHalignment(sceneTitle, javafx.geometry.HPos.CENTER); // vứt cái sceneTitle vào chính giữa cái grid chứ không phải scene
        primaryStage.setTitle("Role Selection");
        HBox hbrole = new HBox(100);
        hbrole.setAlignment(Pos.CENTER);

        Button user = new Button();
        Text txuser = new Text("USER");
        txuser.setFont(Font.font("Tahoma", FontWeight.BOLD, 10));
        user.setGraphic(txuser);
        user.setPrefSize(100, 100);
        user.setStyle("-fx-background-color: WHITE;" +
                "-fx-text-fill: BLACK;");
        DropShadow ds = new DropShadow();
        ds.setOffsetY(3.0f);
        ds.setColor(Color.color(0.4f, 0.4f, 0.4f));
        user.setEffect(ds);

        Button admin = new Button("ADMIN");
        admin.setFont(Font.font("Tahoma", FontWeight.BOLD, 10));
        admin.setStyle("-fx-background-color: white;" + "-fx-text-fill: black;" + "-fx-background-radius: 10;" + "-fx-padding: 10 20 10 20;");
        admin.setPrefSize(100, 100);
        admin.setStyle("-fx-background-color: WHITE;" +
                "-fx-text-fill: BLACK;");
        admin.setEffect(ds);

        hbrole.getChildren().addAll(user, admin);
        grid.add(hbrole, 0, 1);
        grid.setStyle("-fx-background-color: #447D9B");
        Scene scene = new Scene(grid, 900, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
