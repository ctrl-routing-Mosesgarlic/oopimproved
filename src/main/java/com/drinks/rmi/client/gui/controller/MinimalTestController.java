package com.drinks.rmi.client.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MinimalTestController {
    @FXML
    private Label testLabel;
    
    public void initialize() {
        testLabel.setText("Test Label Initialized");
    }
}
