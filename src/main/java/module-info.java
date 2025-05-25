module cz.cvut.fel.pjv.alchemists_quest {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.core;

    opens cz.cvut.fel.pjv.alchemists_quest to javafx.fxml;
    exports cz.cvut.fel.pjv.alchemists_quest;
}