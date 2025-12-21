package cn.xej.api.model;

import lombok.Data;

@Data
public class Disk {
    public String diskName;
    public String diskType = "SSD";
    public Integer diskSize;
}
