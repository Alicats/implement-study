package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * CreateUserResponse 模型类
 */
public class CreateUserResponse extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    
    private String userId;
    
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
}
