package com.example.chatapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;


import com.example.chatapp.R;
import com.example.chatapp.adapters.UserAdapter;
import com.example.chatapp.databinding.ActivityUsersBinding;
import com.example.chatapp.listeners.UserListener;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firestore.admin.v1.Index;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends BaseActivity implements UserListener
{
    private ActivityUsersBinding binding;
    private PreferenceManager preferenceManager;
    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding  = ActivityUsersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        getUsers();
       searchUsers();
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("NonConstantResourceId")
    private void setListeners(){
        binding.bottomNav.setSelectedItemId(R.id.action_friends);
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
            return true;
        });

    }
    // tìm kiếm tuyệt đối qua username
    void searchUsers(){
        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void afterTextChanged(Editable editable) {
                if(editable.toString().equals("")){
                    getUsers();
                }
                else {
                        loading(true);
                        List<User> users = new ArrayList<>();
                        FirebaseFirestore database = FirebaseFirestore.getInstance();
                        database.collection(Constants.KEY_COLLECTION_USERS)
                                .whereEqualTo(Constants.KEY_NAME,editable.toString())
                                .get().addOnCompleteListener(task -> {
                                    loading(false);
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
                                        if(users.size()>0){
                                            UserAdapter userAdapter = new UserAdapter(users,this::afterTextChanged);
                                            // binding.textErrorMessage.setText("");
                                            binding.userRecyclerView.setAdapter(userAdapter);
                                            binding.userRecyclerView.setVisibility(View.VISIBLE);
                                        }else{
                                            showErrorMessage();
                                        }
                                    }
                                });
                    }
            }



            @RequiresApi(api = Build.VERSION_CODES.N)
            private void afterTextChanged(User user) {
                onUserClicked(user);
            }
        });
    }

    public void getUsers(){
        loading(true);
        List<User> users = new ArrayList<>();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get().addOnCompleteListener(task -> {
                    loading(false);
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
                        if(users.size()>0){
                            UserAdapter userAdapter = new UserAdapter(users,this);
                            binding.userRecyclerView.setAdapter(userAdapter);
                            binding.userRecyclerView.setVisibility(View.VISIBLE);
                        }else{
                            showErrorMessage();
                        }
                    }
                });
        }
    private void showErrorMessage(){
        //binding.textErrorMessage.setText(String.format("%s","No user available"));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(boolean isLoading){
        if(isLoading){
            binding.progressBar.setVisibility(View.VISIBLE);
        }else{
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(),ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
        finish();
    }

}