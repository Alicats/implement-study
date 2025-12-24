package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * DescribeUsersRequest 模型类
 */
public class DescribeUsersRequest extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> userIds;
    
    private Integer pageSize;
    
    private Integer pageNum;
    
    
    public Set<String> getUserIds() {
        return userIds;
    }
    
    public void setUserIds(Set<String> userIds) {
        this.userIds = userIds;
    }
    
    public Integer getPageSize() {
        return pageSize;
    }
    
    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
    
    public Integer getPageNum() {
        return pageNum;
    }
    
    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }
    
}
