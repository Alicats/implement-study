package cn.xej.api.models;

import cn.xej.api.common.AbstractModel;

import java.io.Serializable;

/**
 * CreateInstanceRequest 模型类
 */
public class CreateInstanceRequest  extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String labelName;
    
    private String instanceTypeId;

    private Integer bandwidth;

    private String password;
    
    private String ipv4;
    
    
    public String getLabelName() {
        return labelName;
    }
    
    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }
    
    public String getInstanceTypeId() {
        return instanceTypeId;
    }
    
    public void setInstanceTypeId(String instanceTypeId) {
        this.instanceTypeId = instanceTypeId;
    }

    public Integer getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }

    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getIpv4() {
        return ipv4;
    }
    
    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }
    
}
