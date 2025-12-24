package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * CreateUserRequest 模型类
 */
public class CreateUserRequest extends AbstractModel implements Serializable {
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
