package org.example.modul;

public class Util {
    public static String formatCard(Card card) {
        return "Card Number: " + card.getCardNumber() + " | Balance: " + card.getBalance();
    }
}
