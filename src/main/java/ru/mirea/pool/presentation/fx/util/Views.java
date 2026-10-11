package ru.mirea.pool.presentation.fx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.io.IOException;
import java.util.Objects;

public final class Views {
    public record Loaded<T>(Parent root, T controller) { }
    private Views() { }
    public static <T> Loaded<T> load(String name) {
        try {
            var loader = new FXMLLoader(Objects.requireNonNull(Views.class.getResource("/fxml/" + name + ".fxml")));
            return new Loaded<>(loader.load(), loader.getController());
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось открыть окно «" + name + "».", e);
        }
    }
    public static String stylesheet() { return Objects.requireNonNull(Views.class.getResource("/css/pool.css")).toExternalForm(); }
    public static Scene scene(Parent root, double width, double height) {
        var scene = new Scene(root, width, height);
        scene.getStylesheets().add(stylesheet());
        return scene;
    }
}
