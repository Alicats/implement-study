package cn.xej.api.request;

import cn.xej.api.model.Disk;

import java.util.List;
import java.util.Set;

public class CreateInstanceRequest {
    public String labelName;
    public String instanceTypeId;
    public String password;
    public Integer bandwidth;
    public String ipv4;
    public Set<Disk> dataDisks;
    public List<Disk> disks;
    public Boolean enableIpForward = false;
}
