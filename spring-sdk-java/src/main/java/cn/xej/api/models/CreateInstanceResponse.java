package cn.xej.api.models;

import java.io.Serializable;

import java.util.*;

/**
 * CreateInstanceResponse 响应模型
 */
public class CreateInstanceResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceId;

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

}
