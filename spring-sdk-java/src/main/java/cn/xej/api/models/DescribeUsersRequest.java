package cn.xej.api.models;

import java.io.Serializable;

import java.util.*;

/**
 * DescribeUsersRequest 请求模型
 */
public class DescribeUsersRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> userIds;

    public Set<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(Set<String> userIds) {
        this.userIds = userIds;
    }

}
