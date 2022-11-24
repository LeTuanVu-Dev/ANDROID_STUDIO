package com.example.chatapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.example.chatapp.adapters.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.listeners.ApiService;
import com.example.chatapp.listeners.ReloadMessage;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.network.ApiClient;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


@RequiresApi(api = Build.VERSION_CODES.N)
public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private User receivedUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceivedAvailable = false;
    ReloadMessage reloadMessage;
    private String encodedImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        loadReceivedDetails();
        Init();
        listenMessage();
    }

    private void Init() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodeString(receivedUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }


    //: trả về ảnh đã chọn
    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        //lấy địa chỉ của ảnh trong điện thoại
                        Uri imageUri = result.getData().getData();
                        try {
                            //đọc file và gán vào bitmat
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            encodedImage = encodeImage(bitmap);
                            sendMessage();
                        }catch (FileNotFoundException e) {
                            e.printStackTrace(); //thông báo lỗi
                        }
                    }
                }
            }
    );
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == 111 && resultCode == RESULT_OK && data != null){
            Bitmap bitmap = (Bitmap) data.getExtras().get("data");
            encodedImage = encodeImage(bitmap);

            sendMessage();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }



    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_IS_READER_MESSAGE,false);
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receivedUser.id);
       // message.put(Constants.KEY_MESSAGE , binding.inputMessage.getText().toString().trim());
        if(encodedImage != "" && encodedImage != null) {
            message.put(Constants.KEY_MESSAGE_IMAGE, encodedImage);
        } else {
            message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        }
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if(conversionId != null) {
            if(encodedImage != "" && encodedImage != null) {
                updateConversion("Đã gửi một ảnh");
            } else {
                updateConversion(binding.inputMessage.getText().toString());
            }
            encodedImage = "";
            //updateConversion(binding.inputMessage.getText().toString());
        }else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID,receivedUser.id);
            //conversion.put(Constants.KEY_IS_READER_MESSAGE,false);
            conversion.put(Constants.KEY_RECEIVER_NAME,receivedUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE,receivedUser.image);
          //  conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString().trim());
            if(encodedImage != "" && encodedImage != null) {
                conversion.put(Constants.KEY_LAST_MESSAGE, "Đã gửi một ảnh");
            } else {
                conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString());
            }
            conversion.put(Constants.KEY_TIMESTAMP,new Date());
            addConversion(conversion);
        }
        if (!isReceivedAvailable){
            try {
                JSONArray tokens = new JSONArray();
                tokens.put(receivedUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME,preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN,preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString().trim());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOVE_MSG_DATA,data);
                body.put(Constants.REMOVE_MSG_REGISTRATION_IDS,tokens);

                sendNotification(body.toString());
            } catch (JSONException e) {
                showToast(e.getMessage());
            }
        }
        binding.inputMessage.setText(null);
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

private void sendNotification(String messageBody){
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoveMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if(response.isSuccessful()){
                    try {
                        if (response.body()!=null){
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if(responseJson.getInt("failure") == 1){
                                JSONObject error = (JSONObject) results.get(0);
                                showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                   showToast("Notification sent successfully!");
                }else {
                    showToast("Error: "+response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
}
// hiển thị đang onl hay off
    private void listenerAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receivedUser.id
        ).addSnapshotListener(ChatActivity.this, (value, error) -> {
            if(error !=null) {
                return;
            }
            if(value !=null) {
                if(value.getLong(Constants.KEY_AVAILABILITY) !=null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceivedAvailable = availability == 1;
                }
                receivedUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receivedUser.image == null){
                    receivedUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodeString(receivedUser.image));
                    chatAdapter.notifyItemRangeChanged(0,chatMessages.size());
                }
            }
            if(isReceivedAvailable) {
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else {
                binding.textAvailability.setVisibility(View.GONE);
            }

        });
    }

    private void listenMessage() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID , preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID , receivedUser.id )
                .addSnapshotListener(evenListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID , receivedUser.id )
                .whereEqualTo(Constants.KEY_RECEIVER_ID , preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(evenListener);


    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> evenListener = (value, error) -> {
        if(error != null) {
            return;
        }if(value != null) {
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()) {
                if(documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.isReader = documentChange.getDocument().getBoolean(Constants.KEY_IS_READER_MESSAGE);
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    // gửi tin nhắn là ảnh đi
                    chatMessage.messageImage = documentChange.getDocument().getString(Constants.KEY_MESSAGE_IMAGE);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    //chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages , Comparator.comparing(obj -> obj.dateObject));
            if(count == 0) {
                chatAdapter.notifyDataSetChanged();
            }else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(),chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversionId ==null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodeString(String encodeImage) {
        if (encodeImage!=null){
            byte[] bytes = android.util.Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        }
        else {
            return null;
        }
    }

    private void loadReceivedDetails() {
        receivedUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receivedUser.name);
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                Intent intent= new Intent(getApplicationContext(),MainActivity.class);
                startActivityForResult(intent,100);
            }
        });

        binding.layoutSend.setOnClickListener(v -> sendMessage());

        // chọn ảnh
        binding.imageImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickImage.launch(intent);
        });

        // chụp ảnh
        binding.imageCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            // 111 là code phân biệt là giá trị trả về của máy ảnh
            // có thể để số bất kỳ
            startActivityForResult(intent, 111);
        });

        //
        // call user received
        binding.imageInfo.setOnClickListener( v -> {
            receivedUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
            String phonenumberReceived = receivedUser.sdt;
            //showToast(phonenumberReceived);
            Intent intentcall = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phonenumberReceived));
            startActivity(intentcall);
        } );
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String,Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATION).document(conversionId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE,message,
                Constants.KEY_TIMESTAMP,new Date()
        );
    }

    private void checkForConversion() {
        if(chatMessages.size() !=0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receivedUser.id
            );
            checkForConversionRemotely(
                    receivedUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receivedId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATION)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receivedId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);

    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if(task.isSuccessful() && task.getResult() !=null && task.getResult().getDocuments().size() >0) {
            DocumentSnapshot documentSnapshot =   task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        listenerAvailabilityOfReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        listenerAvailabilityOfReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        listenerAvailabilityOfReceiver();

    }
}