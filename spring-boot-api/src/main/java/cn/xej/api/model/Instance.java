package cn.xej.api.model;


import lombok.Data;

@Data
public class Instance {
    public String uuid;
    public String labelName;
    public String instanceType;
    public String status;
    public String ip;
    public String createTime;

}
