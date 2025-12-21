package cn.xej.api.models;

import java.io.Serializable;
import java.util.*;

/**
 * DescribeUsersResponse 模型类
 */
public class DescribeUsersResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<User> userSet;
    
    private Integer totalCount;
    
    
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
