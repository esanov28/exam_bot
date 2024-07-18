package org.example.modul;

public class Card {
    private long userId;
    private String cardNumber;
    private double balance;

    public Card(long userId, String cardNumber, double balance) {
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.balance = balance;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }
}
