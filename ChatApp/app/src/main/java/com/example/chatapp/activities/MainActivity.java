package com.example.chatapp.activities;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.adapters.RecentConversationsAdapter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.listeners.ConversionListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;
    private Boolean isReader = false;
    private String changePass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(getApplicationContext());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadUserDetails();
        getToken();
        setListeners();
        listenConversations();
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
    @SuppressLint("ResourceAsColor")
    private void init(){
        // tạo ra 1 mảng để chứa các cuộc hội thoại của ng dùng
        conversations = new ArrayList<>();
        // khởi tạo rcv
        conversationsAdapter = new RecentConversationsAdapter(conversations,this);
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);

        // khởi tạo FirebaseFirestore
        database = FirebaseFirestore.getInstance();
    }
    //xử lý sự kiện
    private void setListeners(){
        binding.imageSignOut.setOnClickListener(v-> showToat("Oh No! function invalid!"));
        binding.bottomNav.setSelectedItemId(R.id.action_chat);
        binding.bottomNav.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_chat:
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    break;
                case R.id.action_friends:
                    startActivity(new Intent(getApplicationContext(), UsersActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    break;
                case R.id.action_settings:
                    startActivity(new Intent(getApplicationContext(), SettingActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    break;
            }
            return  true;
        });
    }
    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void showToat(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversations(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error)->{
        if(error != null){
            return ;
        }
        if(value != null){
            for (DocumentChange documentChange : value.getDocumentChanges()){
                // nếu có thêm dữ liệu
                if (documentChange.getType()==DocumentChange.Type.ADDED){
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();
                        chatMessage.senderId = senderId;
                        chatMessage.receiverId = receiverId;
                        // Nếu ng dùng là người gửi sẽ chạy đoạn lệnh này
                        if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                            chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                            chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                            chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                            chatMessage.message =documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                        }// ngược lại khi là người nhận sẽ chạy đoạn này
                        else {
                            chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                            chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                            chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                            chatMessage.message =  documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                        }

                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);

                }
                else if (documentChange.getType() == DocumentChange.Type.MODIFIED){
                    for (int i=0;i<conversations.size();i++){
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if(conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)){
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations,(obj1,obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecyclerView.smoothScrollToPosition(0);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private  void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token){
        preferenceManager.putString(Constants.KEY_FCM_TOKEN,token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_USERS).document(
                        preferenceManager.getString(Constants.KEY_USER_ID)
                );
        documentReference.update(Constants.KEY_FCM_TOKEN,token)
                .addOnFailureListener(e -> showToat("Unable to update token"));
    }


    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    // click vào trò chuyện ở màn hình sẽ hiện ra cuộc trò chuyện
    public void onConversionClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}