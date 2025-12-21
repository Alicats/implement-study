package cn.xej.api.models;

import java.io.Serializable;
import java.util.*;

/**
 * DescribeInstancesRequest 模型类
 */
public class DescribeInstancesRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> instanceIds;
    
    private String labelName;
    
    private Integer pageSize;
    
    private Integer pageNum;
    
    
    public Set<String> getInstanceIds() {
        return instanceIds;
    }
    
    public void setInstanceIds(Set<String> instanceIds) {
        this.instanceIds = instanceIds;
    }
    
    public String getLabelName() {
        return labelName;
    }
    
    public void setLabelName(String labelName) {
        this.labelName = labelName;
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
