package cn.xej.api.models;

import java.io.Serializable;
import cn.xej.api.common.AbstractModel;
import java.util.*;

/**
 * Disk 模型类
 */
public class Disk extends AbstractModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private String diskName;
    
    private String diskType;
    
    private Integer diskSize;
    
    
    public String getDiskName() {
        return diskName;
    }
    
    public void setDiskName(String diskName) {
        this.diskName = diskName;
    }
    
    public String getDiskType() {
        return diskType;
    }
    
    public void setDiskType(String diskType) {
        this.diskType = diskType;
    }
    
    public Integer getDiskSize() {
        return diskSize;
    }
    
    public void setDiskSize(Integer diskSize) {
        this.diskSize = diskSize;
    }
    
}
