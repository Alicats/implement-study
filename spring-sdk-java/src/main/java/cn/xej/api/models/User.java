package cn.xej.api.models;

import java.io.Serializable;
import java.util.*;

/**
 * User 模型类
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    
    private String name;
    
    private String email;
    
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
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
