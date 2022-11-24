package com.example.chatapp.adapters;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.chatapp.R;
import com.example.chatapp.databinding.ItemContainerReceivedMessageBinding;
import com.example.chatapp.databinding.ItemContainerSentMessaageBinding;
import com.example.chatapp.listeners.ReloadMessage;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;


import java.util.List;
import java.util.Objects;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static List<ChatMessage> chatMessages;
    private Bitmap receiverProfileImage;
    private final String senderId;
    private static PreferenceManager preferenceManager;
    public static final int VIEW_TYPE_SENT = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;
    int positonDel;
    static FirebaseFirestore database;

    public void setReceiverProfileImage(Bitmap bitmap){
        receiverProfileImage = bitmap;
    }

    public ChatAdapter(List<ChatMessage> chatMessages, Bitmap receiverProfileImage, String senderId) {
        ChatAdapter.chatMessages = chatMessages;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
       // this.reloadMessage = reloadMessage;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        preferenceManager = new PreferenceManager(parent.getContext());
        if(viewType == VIEW_TYPE_SENT){
            return new SentMessageViewHolder(ItemContainerSentMessaageBinding.inflate(
                    LayoutInflater.from(parent.getContext()), parent, false
            ));
        }else {
            return new ReceivedMessageViewHolder(
                    ItemContainerReceivedMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()), parent, false
                    )
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        database = FirebaseFirestore.getInstance();
        if (getItemViewType(position)==VIEW_TYPE_SENT){
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        }
        else {
            ((ReceivedMessageViewHolder) holder).setData(chatMessages.get(position),receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    @Override
    public int getItemViewType(int position) {
        if(chatMessages.get(position).senderId.equals(senderId)){
            return VIEW_TYPE_SENT;
        }else {
            return VIEW_TYPE_RECEIVED;
        }
    }


    public  class SentMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerSentMessaageBinding binding;

        SentMessageViewHolder(ItemContainerSentMessaageBinding itemContainerSentMessaageBinding){
            super(itemContainerSentMessaageBinding.getRoot());
            binding = itemContainerSentMessaageBinding;
            binding.textMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage("Xóa ở phía bạn");
                    builder.setTitle("Question");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            database.collection(Constants.KEY_COLLECTION_CHAT)
                                    .document(Constants.KEY_CHATID)
                                    .delete()
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            for (int a=0;a<chatMessages.size();a++){
                                                if(chatMessages.get(a).getMessage().equals(binding.textMessage.getText().toString().trim()) &&
                                                        chatMessages.get(a).getDateTime().equals(binding.textDateTime.getText().toString())){
                                                    chatMessages.remove(a);
                                                    Snackbar.make(binding.textMessage, "xóa tin nhắn thành công !! ",
                                                            Snackbar.LENGTH_SHORT).show();
                                                }
                                            }
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                        }
                                    });
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    // hiện màu background
                    dialog.getWindow().setBackgroundDrawableResource(R.color.purple_100);
                    dialog.show();
                    return false;
                }
            });

        }


        void setData(ChatMessage chatMessage){
            // nãy làm ở đây
            //binding.textMessage.setText(chatMessage.message);
            if(chatMessage.messageImage != "" && chatMessage.messageImage != null) {
                binding.imageMessage.setImageBitmap(getUserImage(chatMessage.messageImage));
                binding.textMessage.setVisibility(View.INVISIBLE);
            }else {
                binding.textMessage.setText(chatMessage.message);
            }
            // kết thúc code ở đây

            //binding.textDateTime.setText(chatMessage.dateTime);
        }
        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder{
        private final ItemContainerReceivedMessageBinding binding;

        ReceivedMessageViewHolder(ItemContainerReceivedMessageBinding itemContainerSentMessaageBinding){
            super(itemContainerSentMessaageBinding.getRoot());
            binding=itemContainerSentMessaageBinding;

            binding.textMessage.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                    builder.setMessage("Xóa ở phía bạn");
                    builder.setTitle("Question");
                    builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(itemView.getContext(), "đã xóa", Toast.LENGTH_SHORT).show();
                            // ẩn tin nhắn
                                binding.textMessage.setVisibility(View.GONE);
                                binding.textDateTime.setVisibility(View.GONE);
                        }
                    });
                    builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    // hiện màu background
                    dialog.getWindow().setBackgroundDrawableResource(R.color.purple_100);
                    dialog.show();
                    return false;
                }
            });
        }
        void setData(ChatMessage chatMessage,Bitmap receiverProfileImage) {
            // binding.textMessage.setText(chatMessage.message);

            // bắt đầu ở đây
            if(chatMessage.messageImage != "" && chatMessage.messageImage != null) {
                binding.imageMessage.setImageBitmap(getUserImage(chatMessage.messageImage));
                binding.textMessage.setVisibility(View.INVISIBLE);
            }else {
                binding.textMessage.setText(chatMessage.message);
            }
            // kết thức ở đây
           // binding.textDateTime.setText(chatMessage.dateTime);
            binding.imageProfile.setImageBitmap(receiverProfileImage);
        }
        private Bitmap getUserImage(String encodedImage) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
    }
}
