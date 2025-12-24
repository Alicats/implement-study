package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * CreateInstanceRequest 模型类
 */
public class CreateInstanceRequest extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String labelName;
    
    private String instanceTypeId;
    
    private String password;
    
    private Integer bandwidth;
    
    private String ipv4;
    
    private Set<Disk> dataDisks;
    
    private List<Disk> disks;
    
    private Boolean enableIpForward;
    
    
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Integer getBandwidth() {
        return bandwidth;
    }
    
    public void setBandwidth(Integer bandwidth) {
        this.bandwidth = bandwidth;
    }
    
    public String getIpv4() {
        return ipv4;
    }
    
    public void setIpv4(String ipv4) {
        this.ipv4 = ipv4;
    }
    
    public Set<Disk> getDataDisks() {
        return dataDisks;
    }
    
    public void setDataDisks(Set<Disk> dataDisks) {
        this.dataDisks = dataDisks;
    }
    
    public List<Disk> getDisks() {
        return disks;
    }
    
    public void setDisks(List<Disk> disks) {
        this.disks = disks;
    }
    
    public Boolean getEnableIpForward() {
        return enableIpForward;
    }
    
    public void setEnableIpForward(Boolean enableIpForward) {
        this.enableIpForward = enableIpForward;
    }
    
}
