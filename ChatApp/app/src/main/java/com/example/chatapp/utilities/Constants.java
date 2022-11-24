package com.example.chatapp.utilities;

import java.util.HashMap;

public class Constants {
    public static final String KEY_COLLECTION_USERS = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_SDT = "sdt";
    public static final String KEY_PREFERENCE_NAME = "chatAppPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";

    public static final String KEY_USER_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_USER = "user";
    public static final String KEY_IS_READER_MESSAGE= "isReader";

    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_CHATID = "chatId";
    public static final String KEY_CHANGE_PASS = "KEY_CHANGE_PASS";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_STATUS_LANGUAGE = "statusLanguage";
    public static final String KEY_COLLECTION_CONVERSATION = "conversation";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String REMOVE_MSG_AUTHORIZATION = "Authorization";
    public static final String REMOVE_MSG_CONTENT_TYPE = "Content-Type";
    public static final String REMOVE_MSG_DATA = "data";
    public static final String REMOVE_MSG_REGISTRATION_IDS = "registration_ids";


    // hoạt động máy ảnh
    public static final String KEY_MESSAGE_IMAGE = "message_image";

    public static HashMap<String,String> removeMsgHeaders=null;
    public static final HashMap<String,String> getRemoveMsgHeaders(){
        if(removeMsgHeaders==null){
            removeMsgHeaders = new HashMap<>();
            removeMsgHeaders.put(
                    REMOVE_MSG_AUTHORIZATION,
                    "key=AAAA232byeY:APA91bE1CePmvr-14I7K9KKGgdB1-aMyKyvmQMDdRaD86q4jJQCf-L2KSVfwvCgFsXJlduf49n1E9S1wqkY9vHK_hdvzhZfuGhTAYl_ipLyxwPfJ7XDtUEjRowGm8Ov9uaIRRDgmxUaj"
            );
            removeMsgHeaders.put(
                    REMOVE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return removeMsgHeaders;
    }
}
