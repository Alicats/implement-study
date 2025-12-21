package cn.xej.api.models;

import java.io.Serializable;

import java.util.*;

/**
 * DescribeInstancesResponse 响应模型
 */
public class DescribeInstancesResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Instance> instances;

    private Integer totalCount;

    public List<Instance> getInstances() {
        return instances;
    }

    public void setInstances(List<Instance> instances) {
        this.instances = instances;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

}
