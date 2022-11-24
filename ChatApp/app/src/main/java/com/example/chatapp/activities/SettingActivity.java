package com.example.chatapp.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.chatapp.R;
import com.example.chatapp.adapters.UserAdapter;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.databinding.ActivitySettingBinding;
import com.example.chatapp.databinding.LayoutChangePasswordBinding;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SettingActivity extends AppCompatActivity {

    private ActivitySettingBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private FirebaseFirestore database;
    private int selectLanguage;
    private String language;
    private User receivedUser;
    String pass;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        init();
        loadUserDetails();
        setListeners();
       //loadReceivedDetails();
    }
    private void init() {
        binding.bottomNav.setSelectedItemId(R.id.action_settings);
        preferenceManager = new PreferenceManager(getApplicationContext());
        database = FirebaseFirestore.getInstance();
    }

    public void getUsers(){
        List<User> users = new ArrayList<>();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get().addOnCompleteListener(task -> {
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult()!=null){
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            User user = new User();
                            user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                            user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                            user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                            user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                            user.id = queryDocumentSnapshot.getId();
                            users.add(user);
                        }

                    }
                });
    }
    private void setListeners() {
        //hiển thị sdt
       binding.textSDT.setText(preferenceManager.getString(Constants.KEY_SDT));

        binding.buttonLogout.setOnClickListener(v -> signOut());
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
                    break;
            }
            return true;
        });

        // thay đổi avt
        binding.imageProfile.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.confirm);
            builder.setMessage(R.string.are_you_sure_change_picture_group_chat);
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    pickImage.launch(intent);
                    dialogInterface.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(getApplicationContext(), R.string.cancel, Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
            builder.show();

        });

        // thay đổi pass
        binding.buttonChangePassword.setOnClickListener(v -> {
            final Dialog dialog = new Dialog(this);
            LayoutChangePasswordBinding layoutChangePasswordBinding;
            layoutChangePasswordBinding = LayoutChangePasswordBinding.inflate(getLayoutInflater());
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(layoutChangePasswordBinding.getRoot());
            dialog.show();
            Window window = dialog.getWindow();
            if(window == null){
                return;
            }
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            WindowManager.LayoutParams windowAttributes = window.getAttributes();
            windowAttributes.gravity = Gravity.CENTER;
            window.setAttributes(windowAttributes);

            layoutChangePasswordBinding.buttonCancel.setOnClickListener(view -> dialog.dismiss());
            layoutChangePasswordBinding.buttonSave.setOnClickListener(view -> {
                if(layoutChangePasswordBinding.inputPasswordOld.getText().toString().trim().isEmpty()) {
                    showToat(getString(R.string.old_pass_cannot_blank));
                } else if(layoutChangePasswordBinding.inputPasswordNew.getText().toString().trim().isEmpty()) {
                    showToat(getString(R.string.new_pass_cannot_blank));
                }
                // có lỗi ko get đc pass ở firestore
                else if(!layoutChangePasswordBinding.inputPasswordOld.getText().toString().trim().equals(preferenceManager.getString(Constants.KEY_CHANGE_PASS))) {
                    showToat(getString(R.string.old_pass_is_incorrect));
                }
                else if(layoutChangePasswordBinding.inputPasswordOld.getText().toString().trim().equals(layoutChangePasswordBinding.inputPasswordNew.getText().toString().trim())) {
                    showToat("Password cannot be duplicated");
                }
                else {
                    preferenceManager.putString(Constants.KEY_CHANGE_PASS,layoutChangePasswordBinding.inputPasswordNew.getText().toString().trim());
                    DocumentReference documentReference =
                            database.collection(Constants.KEY_COLLECTION_USERS)
                                    .document(preferenceManager.getString(Constants.KEY_USER_ID));
                    documentReference.update(
                            Constants.KEY_PASSWORD, layoutChangePasswordBinding.inputPasswordNew.getText().toString().trim()
                    );
                    preferenceManager.putString(Constants.KEY_PASSWORD, layoutChangePasswordBinding.inputPasswordNew.getText().toString().trim());
                    showToat(getString(R.string.change_pass_success));
                    dialog.dismiss();
                }
            });

        });
        // thay đổi ngôn ngữ
        binding.buttonLanguage.setOnClickListener(v -> {

            final String[] languages = {getString(R.string.english), getString(R.string.vietnamese)};
            AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
            builder.setTitle(R.string.select_language);
            int checkedItem = 0;
            if(preferenceManager.getString(Constants.KEY_STATUS_LANGUAGE) == "vi")
                checkedItem = 1;
            builder.setSingleChoiceItems(languages, checkedItem, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    selectLanguage = i;
                }
            });
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if(selectLanguage == 0) {
                        language = "en";
                    }
                    if(selectLanguage == 1) {
                        language = "vi";
                    }
                    if(language == "" || language == null) {
                        language = "en";
                    }
                    // Cập nhật trạng thái ngôn ngữ
                    preferenceManager.putString(Constants.KEY_STATUS_LANGUAGE, language);
                    // Thay đổi ngôn ngữ
                    setLocale(language);
                    Toast.makeText(SettingActivity.this, getString(R.string.success_update_language), Toast.LENGTH_SHORT).show();
                    dialogInterface.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            builder.show();
        });
    }


    private void setLocale(String language) {
        Resources resources = getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = new Locale(language);
        resources.updateConfiguration(configuration, metrics);
        onConfigurationChanged(configuration);
    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Sau khi thay đổi ngôn ngữ, cập nhật dữ liệu trên activity hiện thời
        // Cách 1
/*        binding.textDarkMode.setText(R.string.dark_mode);
        binding.textChangePass.setText(R.string.change_password);
        binding.textLanguage.setText(R.string.language);
        binding.textLogOut.setText(R.string.log_out);*/
        // Cách 2
       Intent intent = new Intent(this, SettingActivity.class);
        startActivity(intent);
        finish();

    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if(result.getResultCode() == RESULT_OK) {
                    if(result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);

                            DocumentReference documentReference =
                                    database.collection(Constants.KEY_COLLECTION_USERS)
                                            .document(preferenceManager.getString(Constants.KEY_USER_ID));
                            documentReference.update(
                                    Constants.KEY_IMAGE, encodedImage
                            );
                            preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
                            showToat(getString(R.string.you_have_success_update_your_profile_picture));
                        }catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void showToat(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void signOut(){
        showToat("Sign out...");
        FirebaseFirestore database =FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String ,Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToat("Unable to sign out"));
    }
    private void loadUserDetails() {
        binding.textName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }
}