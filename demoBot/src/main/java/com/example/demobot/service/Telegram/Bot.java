package com.example.demobot.service.Telegram;

import com.example.demobot.config.BotConfig;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class Bot extends TelegramLongPollingBot {

    private final BotConfig config;
    private final Map<Long, Boolean> encryptMode = new HashMap<>();
    private final Map<Long, Boolean> decryptMode = new HashMap<>();

    public Bot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "Welcome message"));
        listOfCommands.add(new BotCommand("/encrypt", "Encrypt your message"));
        listOfCommands.add(new BotCommand("/decrypt", "Decrypt your message"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (encryptMode.getOrDefault(chatId, false)) {
                encryptMessage(chatId, message);
                encryptMode.put(chatId, false); // Отключаем режим шифрования после обработки
            } else if (decryptMode.getOrDefault(chatId, false)) {
                decryptMessage(chatId, message);
                decryptMode.put(chatId, false); // Отключаем режим расшифровки после обработки
            } else {
                switch (message) {
                    case "/start":
                        startMessage(chatId);
                        break;
                    case "/encrypt":
                        encMode(chatId);
                        break;
                    case "/decrypt":
                        decMode(chatId);
                        break;
                    default:
                        sendMessage(chatId, "Команда не распознана. Используйте /encrypt для шифрования или /decrypt для расшифровки.");
                }
            }
        }
    }

    private void startMessage(long chatId) {
        String answer = "Привет! Используйте /encrypt для шифрования вашего сообщения или /decrypt для расшифровки.";
        sendMessage(chatId, answer);
    }

    private void encMode(long chatId) {
        String answer = "Введите текст для шифрования.";
        sendMessage(chatId, answer);
        encryptMode.put(chatId, true); // Включаем режим шифрования для текущего чата
    }

    private void decMode(long chatId) {
        String answer = "Введите текст для расшифровки.";
        sendMessage(chatId, answer);
        decryptMode.put(chatId, true); // Включаем режим расшифровки для текущего чата
    }

    private void encryptMessage(long chatId, String plainText) {
        CaesarCipher caesarCipher = new CaesarCipher(3);
        String encryptedText = caesarCipher.encrypt(plainText);
        sendMessage(chatId, "Зашифрованное сообщение: " + encryptedText);
    }

    private void decryptMessage(long chatId, String encryptedText) {
        CaesarCipher caesarCipher = new CaesarCipher(3);
        String decryptedText = caesarCipher.decrypt(encryptedText);
        sendMessage(chatId, "Расшифрованное сообщение: " + decryptedText);
    }

    private void sendMessage(long chatId, String messageToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(messageToSend);
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Вложенный класс для шифрования и расшифровки
    public class CaesarCipher {
        private final int shift;

        public CaesarCipher(int shift) {
            this.shift = shift;
        }

        public String encrypt(String plainText) {
            StringBuilder encryptedText = new StringBuilder();
            for (char c : plainText.toCharArray()) {
                if (Character.isLetter(c)) {
                    char base = Character.isLowerCase(c) ? 'a' : 'A';
                    c = (char) ((c - base + shift) % 26 + base);
                }
                encryptedText.append(c);
            }
            return encryptedText.toString();
        }

        public String decrypt(String encryptedText) {
            StringBuilder decryptedText = new StringBuilder();
            for (char c : encryptedText.toCharArray()) {
                if (Character.isLetter(c)) {
                    char base = Character.isLowerCase(c) ? 'a' : 'A';
                    c = (char) ((c - base - shift + 26) % 26 + base);
                }
                decryptedText.append(c);
            }
            return decryptedText.toString();
        }
    }
}
