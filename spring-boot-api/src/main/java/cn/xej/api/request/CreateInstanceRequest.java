package cn.xej.api.request;

import cn.xej.api.model.Disk;

import java.util.List;

public class CreateInstanceRequest {
    public String labelName;
    public String instanceTypeId;
    public String password;
    public String ipv4;
    public List<Disk> disks;
}
