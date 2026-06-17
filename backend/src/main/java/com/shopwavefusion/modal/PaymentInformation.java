package com.shopwavefusion.modal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PaymentInformation {

    @Column(name = "cardholder_name")
    private String cardholderName;

    @Column(name = "card_number")
    private String cardNumber;

    @Column(name = "payment_method")
    private String paymentMethod;

    public PaymentInformation() {
    }

    public PaymentInformation(String cardholderName, String cardNumber, String paymentMethod) {
        this.cardholderName = cardholderName;
        this.cardNumber = cardNumber;
        this.paymentMethod = paymentMethod;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
