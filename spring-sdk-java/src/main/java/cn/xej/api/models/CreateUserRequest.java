package cn.xej.api.models;

import java.io.Serializable;

import java.util.*;

/**
 * CreateUserRequest 请求模型
 */
public class CreateUserRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;

    private String email;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}
