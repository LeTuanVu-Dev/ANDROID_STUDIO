package com.example.chatapp.models;

import java.util.Date;

public class ChatMessage {
    public String senderId,receiverId,message,messageImage, dateTime,chatId;
    public Date dateObject;
    public Boolean isReader;
    public String conversionId,conversionName,conversionImage;

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }
    public String getDateTime() {
        return dateTime;
    }

}
