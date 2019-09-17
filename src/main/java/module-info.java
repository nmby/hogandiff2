
module xyz.hotchpotch.hogandiff {
    requires java.desktop;
    requires java.xml;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires poi;
    
    opens xyz.hotchpotch.hogandiff to javafx.graphics, javafx.fxml;
}