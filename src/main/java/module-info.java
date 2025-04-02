module cz.cvut.fel.pjv.alchemist_quest {
    requires javafx.controls;
    requires javafx.fxml;


    opens cz.cvut.fel.pjv.alchemist_quest to javafx.fxml;
    exports cz.cvut.fel.pjv.alchemist_quest;
}