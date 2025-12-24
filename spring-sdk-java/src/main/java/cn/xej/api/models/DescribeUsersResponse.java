package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * DescribeUsersResponse 模型类
 */
public class DescribeUsersResponse extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    
    private List<User> userSet;
    
    private Integer totalCount;
    
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public List<User> getUserSet() {
        return userSet;
    }
    
    public void setUserSet(List<User> userSet) {
        this.userSet = userSet;
    }
    
    public Integer getTotalCount() {
        return totalCount;
    }
    
    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
    
}
