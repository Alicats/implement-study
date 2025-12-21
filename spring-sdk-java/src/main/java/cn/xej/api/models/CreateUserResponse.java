package cn.xej.api.models;

import java.io.Serializable;

import java.util.*;

/**
 * CreateUserResponse 响应模型
 */
public class CreateUserResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String userId;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}
