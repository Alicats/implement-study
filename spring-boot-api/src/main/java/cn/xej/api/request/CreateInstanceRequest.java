package cn.xej.api.request;

import cn.xej.api.model.Disk;

import javax.validation.constraints.*;

import java.util.List;
import java.util.Set;

public class CreateInstanceRequest {
    @NotBlank(message = "labnel not blank")
    public String labelName;
    public String instanceTypeId;
    public String password;
    @NotNull(message = "bandwidth not null")
    @Min(value = 1, message = "bandwidth min 1")
    @Max(value = 1000, message = "bandwidth max 1000")
    public Integer bandwidth;
    public String ipv4;
    @NotEmpty(message = "dataDisks not empty")
    public Set<Disk> dataDisks;
    public List<Disk> disks;
    public Boolean enableIpForward = false;
}
