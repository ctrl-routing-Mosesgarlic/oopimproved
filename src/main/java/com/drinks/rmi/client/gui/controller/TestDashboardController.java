package com.drinks.rmi.client.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * Simplified test controller without inheritance
 */
public class TestDashboardController {
    
    @FXML
    private Label welcomeLabel;
    
    // Temporarily removed totalLabel for testing
    // @FXML
    // private Label totalLabel;
    
    public void initialize() {
        welcomeLabel.setText("Test Dashboard Loaded");
        // totalLabel.setText("$0.00");
    }
}
