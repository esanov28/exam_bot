package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class MyTelegramBot extends TelegramLongPollingBot {
    private final String BOT_USERNAME = "transfer_g44_bot";
    private final String BOT_TOKEN = "7302809882:AAF4Ew1Y_SaOuCmAMokZYJoNLsNssdeQ32o";

    private final Map<Long, List<Card>> userCards = new HashMap<>();
    private final Map<Long, Boolean> awaitingCardNumber = new HashMap<>();
    private final Map<Long, String> temporaryCardNumber = new HashMap<>();
    private final Map<Long, TransferSession> pendingTransfers = new HashMap<>();
    private final Map<Long, String> pendingDeposit = new HashMap<>();
    private final List<TransferRecord> transferHistory = new ArrayList<>();

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleIncomingMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleIncomingMessage(Message message) {
        String chatId = message.getChatId().toString();
        String text = message.getText();

        if (awaitingCardNumber.containsKey(message.getChatId()) && awaitingCardNumber.get(message.getChatId())) {
            temporaryCardNumber.put(message.getChatId(), text);
            awaitingCardNumber.put(message.getChatId(), false);
            sendMessage(chatId, "Balansni kiriting:");
        } else if (temporaryCardNumber.containsKey(message.getChatId())) {
            try {
                double balance = Double.parseDouble(text);
                addCard(message.getChatId(), temporaryCardNumber.get(message.getChatId()), balance);
                temporaryCardNumber.remove(message.getChatId());
                sendMessage(chatId, "Karta muvaffaqiyatli qo'shildi.");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Noto'g'ri . Iltimos, raqam kiriting.");
            }
        } else if (pendingTransfers.containsKey(message.getChatId())) {
            try {
                double amount = Double.parseDouble(text);
                performTransfer(message.getChatId(), amount);
                pendingTransfers.remove(message.getChatId());
                sendMessage(chatId, "O'tkazma muvaffaqiyatli amalga oshirildi.");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Noto'g'ri Iltimos, raqam kiriting.");
            }
        } else if (pendingDeposit.containsKey(message.getChatId())) {
            try {
                double amount = Double.parseDouble(text);
                performDeposit(message.getChatId(), pendingDeposit.get(message.getChatId()), amount);
                pendingDeposit.remove(message.getChatId());
                sendMessage(chatId, "Depozit muvaffaqiyatli qo'shildi");
            } catch (NumberFormatException e) {
                sendMessage(chatId, "Noto'g'ri Iltimos, raqam kiriting.");
            }
        } else {
            switch (text) {
                case "/start":
                    sendMainMenu(chatId);
                    break;
                case "Mening Kartalarim":
                    showMyCards(chatId);
                    break;
                case "Karta Qo'shish":
                    promptForCardNumber(chatId);
                    break;
                case "O'tkazma":
                    showSourceCardSelection(chatId);
                    break;
                case "Tarix":
                    showHistory(chatId);
                    break;
                case "Depozit":
                    showDepositCardSelection(chatId);
                    break;
                default:
                    sendMessage(chatId, "Noma'lum buyruq. Iltimos, menyu tugmalaridan foydalaning.");
                    break;
            }
        }
    }

    private void sendMainMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("bo'lim tanlang ");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Mening Kartalarim"));
        row1.add(new KeyboardButton("Karta Qo'shish"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("O'tkazma"));
        row2.add(new KeyboardButton("Tarix"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Depozit"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showMyCards(String chatId) {
        List<Card> cards = userCards.getOrDefault(Long.parseLong(chatId), new ArrayList<>());
        StringBuilder response = new StringBuilder("Sizning kartalaringiz:\n");
        for (Card card : cards) {
            response.append(card.toString()).append("\n");
        }
        sendMessage(chatId, response.toString());
    }

    private void promptForCardNumber(String chatId) {
        awaitingCardNumber.put(Long.parseLong(chatId), true);
        sendMessage(chatId, "Iltimos, karta raqamini kiriting");
    }

    private void addCard(Long chatId, String cardNumber, double balance) {
        userCards.computeIfAbsent(chatId, k -> new ArrayList<>()).add(new Card(cardNumber, balance));
    }

    private void handleCallbackQuery(Update update) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String data = update.getCallbackQuery().getData();

        if (data.startsWith("source_")) {
            String cardNumber = data.substring("source_".length());
            pendingTransfers.put(update.getCallbackQuery().getMessage().getChatId(), new TransferSession(cardNumber));
            sendMessage(chatId, "qaysi kartaga utkazishni tanlang:");
            showDestinationCardSelection(chatId, cardNumber);
        } else if (data.startsWith("destination_")) {
            String cardNumber = data.substring("destination_".length());
            TransferSession session = pendingTransfers.get(update.getCallbackQuery().getMessage().getChatId());
            session.setDestinationCard(cardNumber);
            sendMessage(chatId, "O'tkazish uchun miqdorini kiriting ");
        } else if (data.startsWith("deposit_")) {
            String cardNumber = data.substring("deposit_".length());
            pendingDeposit.put(update.getCallbackQuery().getMessage().getChatId(), cardNumber);
            sendMessage(chatId, "Depozit miqdorini kiriting");
        }
    }

    private void showSourceCardSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Qaysi kartadan o'kazishni tanlang:");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<Card> cards = userCards.getOrDefault(Long.parseLong(chatId), new ArrayList<>());
        for (Card card : cards) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(card.toString());
            button.setCallbackData("source_" + card.getCardNumber());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showDestinationCardSelection(String chatId, String sourceCard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("otkaziladigan kartani tanlang:");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<Card> cards = userCards.getOrDefault(Long.parseLong(chatId), new ArrayList<>());
        for (Card card : cards) {
            if (!card.getCardNumber().equals(sourceCard)) {
                List<InlineKeyboardButton> rowInline = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(card.toString());
                button.setCallbackData("destination_" + card.getCardNumber());
                rowInline.add(button);
                rowsInline.add(rowInline);
            }
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showDepositCardSelection(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Depozit qilish uchun kartani tanlang:");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<Card> cards = userCards.getOrDefault(Long.parseLong(chatId), new ArrayList<>());
        for (Card card : cards) {
            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(card.toString());
            button.setCallbackData("deposit_" + card.getCardNumber());
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void showHistory(String chatId) {
        StringBuilder history = new StringBuilder("Barcha o'tkazmalar:\n");
        for (TransferRecord record : transferHistory) {
            history.append(record.toString()).append("\n");
        }
        sendMessage(chatId, history.toString());
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void performTransfer(Long chatId, double amount) {
        TransferSession session = pendingTransfers.get(chatId);
        if (session != null) {
            List<Card> cards = userCards.get(chatId);
            Card sourceCard = null;
            Card destinationCard = null;
            for (Card card : cards) {
                if (card.getCardNumber().equals(session.getSourceCard())) {
                    sourceCard = card;
                }
                if (card.getCardNumber().equals(session.getDestinationCard())) {
                    destinationCard = card;
                }
            }
            if (sourceCard != null && destinationCard != null && sourceCard.getBalance() >= amount) {
                sourceCard.decreaseBalance(amount);
                destinationCard.increaseBalance(amount);
                transferHistory.add(new TransferRecord(sourceCard.getCardNumber(), destinationCard.getCardNumber(), amount));
            } else {
                sendMessage(chatId.toString(), "O'tkazma amalga oshmadi. Balans va karta raqamlarini tekshiring.");
            }
        }
    }

    private void performDeposit(Long chatId, String cardNumber, double amount) {
        List<Card> cards = userCards.get(chatId);
        if (cards != null) {
            for (Card card : cards) {
                if (card.getCardNumber().equals(cardNumber)) {
                    card.increaseBalance(amount);
                    break;
                }
            }
        }
    }

    static class Card {
        private final String cardNumber;
        private double balance;

        public Card(String cardNumber, double balance) {
            this.cardNumber = cardNumber;
            this.balance = balance;
        }

        public String getCardNumber() {
            return cardNumber;
        }

        public double getBalance() {
            return balance;
        }

        public void decreaseBalance(double amount) {
            this.balance -= amount;
        }

        public void increaseBalance(double amount) {
            this.balance += amount;
        }

        @Override
        public String toString() {
            return cardNumber + " (Balans: " + balance + ")";
        }
    }

    static class TransferSession {
        private final String sourceCard;
        private String destinationCard;

        public TransferSession(String sourceCard) {
            this.sourceCard = sourceCard;
        }

        public String getSourceCard() {
            return sourceCard;
        }

        public String getDestinationCard() {
            return destinationCard;
        }

        public void setDestinationCard(String destinationCard) {
            this.destinationCard = destinationCard;
        }
    }

    static class TransferRecord {
        private final String sourceCard;
        private final String destinationCard;
        private final double amount;

        public TransferRecord(String sourceCard, String destinationCard, double amount) {
            this.sourceCard = sourceCard;
            this.destinationCard = destinationCard;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "kartalarim: " + sourceCard + " Manzil " + destinationCard + " Miqdor " + amount;
        }
    }
}