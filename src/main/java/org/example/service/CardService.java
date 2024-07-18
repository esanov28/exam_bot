package org.example.service;

import org.example.modul.Card;

import java.util.ArrayList;
import java.util.List;

public class CardService {
    private List<Card> cards = new ArrayList<>();

    public void addCard(long userId, String cardNumber, double balance) {
        cards.add(new Card(userId, cardNumber, balance));
    }

    public List<Card> getCardsForUser(long userId) {
        List<Card> userCards = new ArrayList<>();
        for (Card card : cards) {
            if (card.getUserId() == userId) {
                userCards.add(card);
            }
        }
        return userCards;
    }

    public void transfer(long userId, String fromCardNumber, double amount) {
        Card fromCard = findCardByNumber(userId, fromCardNumber);
        if (fromCard != null && fromCard.getBalance() >= amount) {
            fromCard.setBalance(fromCard.getBalance() - amount);
        }
    }

    public void deposit(long userId, String cardNumber, double amount) {
        Card card = findCardByNumber(userId, cardNumber);
        if (card != null) {
            card.setBalance(card.getBalance() + amount);
        }
    }

    private Card findCardByNumber(long userId, String cardNumber) {
        return cards.stream()
                .filter(card -> card.getUserId() == userId && card.getCardNumber().equals(cardNumber))
                .findFirst()
                .orElse(null);
    }
}
