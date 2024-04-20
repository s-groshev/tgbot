package ru.angara.kitchen.service;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.angara.kitchen.config.BotConfig;
import ru.angara.kitchen.model.Ads;
import ru.angara.kitchen.model.AdsRepository;
import ru.angara.kitchen.model.User;
import ru.angara.kitchen.model.UserRepository;

import java.sql.Timestamp;
import java.util.*;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private AdsRepository adsRepository;
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;

    static final String HELP_TEXT = "Этот бот создает \n\n" +
            "Type /start for ...\n\n" +
            "Type /mydata for ...\n\n";
    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";
    static final String ERROR_TEXT = "ERROR ";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start","Запустить бот"));
        listOfCommands.add(new BotCommand("/donate","Отправить донат"));
        listOfCommands.add(new BotCommand("/notification","Настроить уведомление о донатах"));
        listOfCommands.add(new BotCommand("/report_of_donate","Отчетность по донатам"));
        listOfCommands.add(new BotCommand("/goods_shopping","Пожелания по закупке"));
        listOfCommands.add(new BotCommand("/report_of_purchase","Запрос отчетности по закупкам"));
        listOfCommands.add(new BotCommand("/receipt","Подкрепление фото чеков от заказчика"));
        listOfCommands.add(new BotCommand("/help","Инструкции"));
//        listOfCommands.add(new BotCommand("/register","РЕГИСТРАТУРА"));
//        listOfCommands.add(new BotCommand("/settings","settings your preference"));


        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(),null));
        } catch (TelegramApiException e) {
            e.printStackTrace();
            log.error("Error settings bot's command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageTest = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageTest.contains("/send")
                    && config.getBotOwner().equals(chatId)) {
                String textToSend = EmojiParser.parseToUnicode(
                        messageTest.substring(messageTest.indexOf(" ")));
                Iterable<User> users = userRepository.findAll();
                for (User user: users) {
                    onlyText(user.getChatId(), textToSend);
                }
            } else {
                switch (messageTest) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId,update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        onlyText(chatId,HELP_TEXT);
                        break;
                    case "/register":
                        TextAndButtonUnderMessage(chatId, "asdaasdasd");
                        break;
                    case "/donate":
                        donateMessage(chatId);
                        break;
                    case "Донат":
                        donateMessage(chatId);
                        break;
                    case "/notification":
                        notificationMessage(chatId);
                        break;
                    case "/reporting":
//                        TextAndButtonUnderMessage();
                        break;
                    case "/goods_shopping":
//                        TextAndButtonUnderMessage();
                        break;
                    case "/report_of_purchase":
//                        TextAndButtonUnderMessage();
                        break;
                    case "/receipt":
//                        TextAndButtonUnderMessage();
                        break;
                default: onlyText(chatId, "Ничего");
                    break;
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            String callbackDate = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackDate.equals("https://www.sberbank.com/sms/pbpn?requisiteNumber=9228354869")) {
                String text = "Нажал ДА";
                executeMessageText(text,chatId,messageId);

            } else if (callbackDate.equals(NO_BUTTON)) {
                String text = "Нажал НОУ";
                executeMessageText(text,chatId,messageId);
            }

            if (callbackDate.startsWith("setInterval") && callbackDate.contains("#")) {
                String[] str = callbackDate.split("#");
                String strInterval = str[2];
                Long interval = Long.parseLong(strInterval);
                Optional<User> oUser = userRepository.findById(chatId);
                User user = oUser.get();
                user.setInter(interval);
                userRepository.save(user);
                onlyText(chatId, "Спасибо!!");
            }

        }
    }

    private void executeMessageText(String text, long chatId, long messageId ){
        //  Отправить сообщение
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void donateMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Выберите способ оплаты");

        HashMap<String,String> donateMap = new HashMap<>();
        donateMap.put("Сбербанк онлайн","https://www.sberbank.com/sms/pbpn?requisiteNumber=9228354869");
        donateMap.put("Тинькофф","https://www.tinkoff.ru/rm/kubagushev.bagautdin1/IyLRW53002");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String donate: donateMap.keySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setUrl(donateMap.get(donate));
            button.setText(donate);
            button.setCallbackData(donateMap.get(donate));
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowInLine.add(button);
            rowsInline.add(rowInLine);
        }

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void notificationMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));

        Optional<User> oUser = userRepository.findById(chatId);
        User user = oUser.get();
        String interval="";
        String notAt="";

        if (user.getInter() != null ) interval = String.valueOf(user.getInter());
        if (user.getNotificationAt() != null ) notAt = String.valueOf(user.getNotificationAt());

        message.setText("Дорогой пир! " + interval + " " + notAt);

        HashMap<String,String> donateMap = new HashMap<>();
        donateMap.put("Неделя","setInterval#" + chatId + "#" + 604_800_000);
        donateMap.put("Месяц","setInterval#" + chatId + "#" + "2592000000");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (String donate: donateMap.keySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(donate);
            button.setCallbackData(donateMap.get(donate));
            List<InlineKeyboardButton> rowInLine = new ArrayList<>();
            rowInLine.add(button);
            rowsInline.add(rowInLine);
        }

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = EmojiParser.parseToUnicode("Hi, :blush: " + name +
                ",nice to meet you! " + "\uD83D\uDD25");
//        https://emojipedia.org/fire
        startMessage(chatId, answer);
        log.info("Зашел user" + name);
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            Long chatId = msg.getChatId();
            Chat chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            user.setNotificationAt(new Timestamp(System.currentTimeMillis()));
            user.setInter(Long.MAX_VALUE);

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void TextAndButtonUnderMessage(long chatId, String newText) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(newText);

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        InlineKeyboardButton maybeButton = new InlineKeyboardButton();
        noButton.setText("Maybe");
        noButton.setCallbackData(NO_BUTTON);

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInline.add(rowInLine);
        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);
    }

    private void startMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Донат");
        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("Hunter");
        row.add("Mioao");
        row.add("___)))_--__");
        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);
    }

    private void onlyText(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() {
        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        //для рассылки рекламы
        for (Ads ad: ads) {
            for (User user : users) {
                //чтобы не доставало
                onlyText(user.getChatId(), ad.getAd());
            }
        }

        //для рассылки увеомлений о донате
        for (User user : users) {
            if (user.getRegisteredAt() != null &&
                    new Date().getTime() - user.getRegisteredAt().getTime() > user.getInter()) {
                user.setNotificationAt(new Timestamp(System.currentTimeMillis()));
                onlyText(user.getChatId(), "Дорогой пир! Пришло время донатить!");
                donateMessage(user.getChatId());
            }
        }
    }


    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        super.onUpdatesReceived(updates);
    }
}
