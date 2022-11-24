package com.example.chatapp.models;

import java.io.Serializable;

public class User implements Serializable {
    public String name,image,email,token,id,sdt;

    public User(String name, String image, String email, String token, String id) {
        this.name = name;
        this.image = image;
        this.email = email;
        this.token = token;
        this.id = id;
    }

    public User() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
