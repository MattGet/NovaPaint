module com.example.paint {
    requires javafx.controls;
    requires javafx.graphics;
    // remove if not using FXML:
    requires javafx.fxml;
    // needed for SwingFXUtils (javafx.embed.swing)
    requires javafx.swing;
    // needed for javax.imageio.ImageIO, BufferedImage, etc.
    requires java.desktop;

    exports com.example.paint;
}
