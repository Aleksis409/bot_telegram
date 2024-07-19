package com.javarush.telegram;

import com.javarush.telegram.ChatGPTService;
import com.javarush.telegram.DialogMode;
import com.javarush.telegram.MultiSessionTelegramBot;
import com.javarush.telegram.UserInfo;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.ArrayList;

public class TinderBoltApp extends MultiSessionTelegramBot {
    public static final String TELEGRAM_BOT_NAME = "RoboCarTestBot"; //TODO: добавь имя бота в кавычках
    public static final String TELEGRAM_BOT_TOKEN = "7047468687:AAF1LOkbaGdaUSHooytcSQtSa33ho74DjHw"; //TODO: добавь токен бота в кавычках
    public static final String OPEN_AI_TOKEN = "gpt:VVyIpbApebyVWX9mhqiuJFkblB3TBSyTBLMJZKjUe3toTRrz"; //TODO: добавь токен ChatGPT в кавычках

    private ChatGPTService chatGPTService = new ChatGPTService(OPEN_AI_TOKEN);
    private DialogMode curerentMode = null;
    private ArrayList<String> list = new ArrayList<>();

    private UserInfo userInfoProfile;
    private UserInfo userInfoOpener;
    private int questionsCount;

    public TinderBoltApp() {
        super(TELEGRAM_BOT_NAME, TELEGRAM_BOT_TOKEN);
    }

    @Override
    public void onUpdateEventReceived(Update update) {
        //TODO: основной функционал бота будем писать здесь
        String message = getMessageText();

        if (message.equals("/start")) {
            curerentMode = DialogMode.MAIN;
            sendPhotoMessage("main");
            String text = loadMessage("main");
            sendTextMessage(text);

            showMainMenu("главное меню бота", "/start",
                    "генерация Tinder-профля \uD83D\uDE0E", "/profile",
                    "сообщение для знакомства \uD83E\uDD70", "/opener",
                    "переписка от вашего имени \uD83D\uDE08", "/message",
                    "переписка со звездами \uD83D\uDD25", "/date",
                    "задать вопрос чату GPT \uD83E\uDDE0", "/gpt"
            );
            return;
        }
        // command GPT
        if (message.equals("/gpt")) {
            curerentMode = DialogMode.GPT;
            sendPhotoMessage("gpt");
            String text = loadMessage("gpt");
            sendTextMessage(text);
            return;
        }

        if (curerentMode == DialogMode.GPT && !isMessageCommand()) {
            String prompt = loadPrompt("gpt");
            Message msg = sendTextMessage("Подождите, чат GPT думает...\uD83E\uDD13");
            String answer = chatGPTService.sendMessage(prompt, message);
            updateTextMessage(msg, answer);
            return;
        }

        // command DATE
        if (message.equals("/date")) {
            curerentMode = DialogMode.DATE;
            sendPhotoMessage("date");
            String text = loadMessage("date");
            sendTextButtonsMessage(text,
                    "Ариана Гранде", "date_grande",
                    "Марго Робби", "date_robbie",
                    "Зендея", "date_zendaya",
                    "Райан Гослинг", "date_gosling",
                    "Том Харди", "date_hardy");
            return;
        }

        if (curerentMode == DialogMode.DATE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("date_")) {
                sendPhotoMessage(query);
                sendTextMessage("Отличный выбор! \nТвоя задача пригласить собеседника на свидание ❤\uFE0F за 5 сообщений");

                String prompt = loadPrompt(query);
                chatGPTService.setPrompt(prompt);
                return;
            }

            Message msg = sendTextMessage("Подождите, собеседник пишет ответ...\uD83D\uDD8A");
            String answer = chatGPTService.addMessage(message);
            updateTextMessage(msg, answer);
            return;
        }

        // command MESSAGE
        if (message.equals("/message")) {
            curerentMode = DialogMode.MESSAGE;
            sendPhotoMessage("message");
            String text = loadMessage("message");
            sendTextButtonsMessage(text,
                    "Следующее сообщение", "message_next",
                    "Пригласить на свидание", "message_date");
            return;
        }

        if (curerentMode == DialogMode.MESSAGE && !isMessageCommand()) {
            String query = getCallbackQueryButtonKey();
            if (query.startsWith("message_")) {
                String prompt = loadPrompt(query);
                String userChatHistory = String.join("\n\n", list);

                Message msg = sendTextMessage("Подождите, чат GPT генерирует ответ...\uD83E\uDDE0");
                String answer = chatGPTService.sendMessage(prompt, userChatHistory);
                updateTextMessage(msg, answer);
            }
            list.add(message);
            return;
        }

        // command PROFILE
        if (message.equals("/profile")) {
            curerentMode = DialogMode.PROFILE;
            sendPhotoMessage("profile");
            String text = loadMessage("profile");
            sendTextMessage(text);

            userInfoProfile = new UserInfo();
            questionsCount = 1;
            sendTextMessage(questionsCount + ". " + "Сколько вам лет?");
            return;
        }

        if (curerentMode == DialogMode.PROFILE && !isMessageCommand()) {
            switch (questionsCount) {
                case 1:
                    userInfoProfile.age = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Кем вы работаете?");
                    return;
                case 2:
                    userInfoProfile.occupation = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "У вас есть хобби?");
                    return;
                case 3:
                    userInfoProfile.hobby = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Что вам Не нравится в людях?");
                    return;
                case 4:
                    userInfoProfile.annoys = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Цели знакомства?");
                    return;
                case 5:
                    userInfoProfile.goals = message;

                    String aboutMySelf = userInfoProfile.toString();
                    String prompt = loadPrompt("profile");
                    Message msg = sendTextMessage("Подождите, чат GPT генерирует ответ...\uD83E\uDDE0");
                    String answer = chatGPTService.sendMessage(prompt, aboutMySelf);
                    updateTextMessage(msg, answer);
            }
            return;
        }

        // command OPENER
        if (message.equals("/opener")) {
            curerentMode = DialogMode.OPENER;
            sendPhotoMessage("opener");
            String text = loadMessage("opener");
            sendTextMessage(text);

            userInfoOpener = new UserInfo();
            questionsCount =1;
            sendTextMessage(questionsCount + ". " + "Имя человека, с кех хочешь познакомиться?");
            return;
        }

        if (curerentMode == DialogMode.OPENER && !isMessageCommand()) {
            switch (questionsCount) {
                case 1:
                    userInfoOpener.name = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Сколько ему/ей лет?");
                    return;
                case 2:
                    userInfoOpener.age = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Есть ли у него/неё хобби?");
                    return;
                case 3:
                    userInfoOpener.hobby = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Кем он/она работает?");
                    return;
                case 4:
                    userInfoOpener.occupation = message;
                    questionsCount += 1;
                    sendTextMessage(questionsCount + ". " + "Цель знакомства?");
                    return;
                case 5:
                    userInfoOpener.goals = message;

                    String aboutFriend = message;
                    String prompt = loadPrompt("opener");
                    Message msg = sendTextMessage("Подождите, чат GPT генерирует ответ...\uD83E\uDDE0");
                    String answer = chatGPTService.sendMessage(prompt, aboutFriend);
                    updateTextMessage(msg, answer);
                    return;
            }
            return;
        }

        sendTextMessage("*Привет*");
        sendTextMessage("_Привет_");


        sendTextMessage("Вы написали " + message);

        sendTextButtonsMessage("Выберите режим работы",
                "Старт", "start",
                "Стоп", "stop");

    }

    public static void main(String[] args) throws TelegramApiException {
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(new TinderBoltApp());
    }
}
