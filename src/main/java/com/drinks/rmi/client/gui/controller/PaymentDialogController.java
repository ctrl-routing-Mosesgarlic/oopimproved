package com.drinks.rmi.client.gui.controller;

import com.drinks.rmi.dto.PaymentResultDTO;
import com.drinks.rmi.interfaces.PaymentService;
import com.drinks.rmi.dto.UserDTO;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Year;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the payment dialog
 */
public class PaymentDialogController implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(PaymentDialogController.class);
    
    @FXML private Label orderIdLabel;
    @FXML private Label branchLabel;
    @FXML private Label itemCountLabel;
    @FXML private Label totalAmountLabel;
    
    @FXML private ToggleGroup paymentMethodGroup;
    @FXML private RadioButton cardPaymentRadio;
    @FXML private RadioButton mobileMoneyRadio;
    
    @FXML private VBox cardDetailsPane;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardholderNameField;
    @FXML private ComboBox<String> expiryMonthCombo;
    @FXML private ComboBox<Integer> expiryYearCombo;
    @FXML private TextField cvvField;
    
    @FXML private VBox mobileMoneyPane;
    @FXML private TextField phoneNumberField;
    
    @FXML private Button cancelButton;
    @FXML private Button payButton;
    @FXML private StackPane processingPane;
    
    private UserDTO currentUser;
    private PaymentService paymentService;
    private Long orderId;
    private String branchName;
    private int itemCount;
    private BigDecimal totalAmount;
    
    private CompletableFuture<PaymentResultDTO> paymentResultFuture = new CompletableFuture<>();
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize month dropdown (01-12)
        for (int i = 1; i <= 12; i++) {
            expiryMonthCombo.getItems().add(String.format("%02d", i));
        }
        expiryMonthCombo.getSelectionModel().select(0);
        
        // Initialize year dropdown (current year + 10 years)
        int currentYear = Year.now().getValue();
        for (int i = 0; i < 10; i++) {
            expiryYearCombo.getItems().add(currentYear + i);
        }
        expiryYearCombo.getSelectionModel().select(0);
        
        // Set up payment method toggle listeners
        cardPaymentRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            cardDetailsPane.setVisible(newVal);
            cardDetailsPane.setManaged(newVal);
        });
        
        mobileMoneyRadio.selectedProperty().addListener((obs, oldVal, newVal) -> {
            mobileMoneyPane.setVisible(newVal);
            mobileMoneyPane.setManaged(newVal);
        });
    }
    
    /**
     * Set up the payment dialog with order details
     */
    public void setupPayment(UserDTO user, PaymentService service, Long orderId, 
                            String branchName, int itemCount, BigDecimal totalAmount) {
        this.currentUser = user;
        this.paymentService = service;
        this.orderId = orderId;
        this.branchName = branchName;
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        
        // Update UI with order details
        orderIdLabel.setText(orderId.toString());
        branchLabel.setText(branchName);
        itemCountLabel.setText(String.valueOf(itemCount));
        totalAmountLabel.setText(String.format("$%.2f", totalAmount));
    }
    
    /**
     * Handle the payment button click
     */
    @FXML
    private void handlePayment() {
        if (!validatePaymentForm()) {
            return;
        }
        
        // Show processing indicator
        processingPane.setVisible(true);
        processingPane.setManaged(true);
        payButton.setDisable(true);
        cancelButton.setDisable(true);
        
        // Get payment method and details
        String paymentMethod = cardPaymentRadio.isSelected() ? "CARD" : "MOBILE_MONEY";
        String paymentDetails = getPaymentDetails();
        
        // Process payment in background thread
        Task<PaymentResultDTO> paymentTask = new Task<PaymentResultDTO>() {
            @Override
            protected PaymentResultDTO call() throws Exception {
                return paymentService.processPayment(
                    currentUser, 
                    orderId, 
                    totalAmount, 
                    paymentMethod, 
                    paymentDetails
                );
            }
            
            @Override
            protected void succeeded() {
                PaymentResultDTO result = getValue();
                handlePaymentResult(result);
            }
            
            @Override
            protected void failed() {
                Throwable exception = getException();
                logger.error("Payment processing error", exception);
                
                Platform.runLater(() -> {
                    processingPane.setVisible(false);
                    processingPane.setManaged(false);
                    payButton.setDisable(false);
                    cancelButton.setDisable(false);
                    
                    showAlert(
                        Alert.AlertType.ERROR,
                        "Payment Error",
                        "An error occurred while processing your payment: " + 
                        (exception.getMessage() != null ? exception.getMessage() : "Unknown error")
                    );
                    
                    paymentResultFuture.completeExceptionally(exception);
                });
            }
        };
        
        new Thread(paymentTask).start();
    }
    
    /**
     * Handle payment result
     */
    private void handlePaymentResult(PaymentResultDTO result) {
        Platform.runLater(() -> {
            processingPane.setVisible(false);
            processingPane.setManaged(false);
            
            if ("SUCCESS".equals(result.getStatus())) {
                showAlert(
                    Alert.AlertType.INFORMATION,
                    "Payment Successful",
                    "Your payment was processed successfully!\n\n" +
                    "Transaction ID: " + result.getTransactionId() + "\n" +
                    "Amount: $" + result.getAmount() + "\n" +
                    "Status: " + result.getStatus()
                );
                
                paymentResultFuture.complete(result);
                closeDialog();
            } else {
                payButton.setDisable(false);
                cancelButton.setDisable(false);
                
                showAlert(
                    Alert.AlertType.ERROR,
                    "Payment Failed",
                    "Your payment could not be processed.\n\n" +
                    "Reason: " + result.getMessage() + "\n" +
                    "Please try again or use a different payment method."
                );
                
                paymentResultFuture.complete(result);
            }
        });
    }
    
    /**
     * Handle the cancel button click
     */
    @FXML
    private void handleCancel() {
        paymentResultFuture.complete(null);
        closeDialog();
    }
    
    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Get the payment result future
     */
    public CompletableFuture<PaymentResultDTO> getPaymentResultFuture() {
        return paymentResultFuture;
    }
    
    /**
     * Validate the payment form
     */
    private boolean validatePaymentForm() {
        if (cardPaymentRadio.isSelected()) {
            // Validate card details
            if (cardNumberField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a card number");
                return false;
            }
            
            if (cardholderNameField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter the cardholder name");
                return false;
            }
            
            if (cvvField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter the CVV");
                return false;
            }
        } else {
            // Validate mobile money details
            if (phoneNumberField.getText().trim().isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Validation Error", "Please enter a phone number");
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Get payment details as a formatted string
     */
    private String getPaymentDetails() {
        if (cardPaymentRadio.isSelected()) {
            return String.format(
                "CARD:%s:%s:%s/%s:%s",
                cardNumberField.getText().trim().replaceAll("\\s+", ""),
                cardholderNameField.getText().trim(),
                expiryMonthCombo.getValue(),
                expiryYearCombo.getValue(),
                cvvField.getText().trim()
            );
        } else {
            return String.format(
                "MOBILE:%s",
                phoneNumberField.getText().trim().replaceAll("\\s+", "")
            );
        }
    }
    
    /**
     * Show an alert dialog
     */
    private void showAlert(Alert.AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
