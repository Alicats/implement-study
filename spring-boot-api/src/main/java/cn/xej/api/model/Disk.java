package cn.xej.api.model;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class Disk {
    @NotBlank(message = "diskName not blank")
    public String diskName;
    public String diskType = "SSD";
    @NotNull
   @Min(value = 1, message = "diskSize min 1")
    @Max(value = 10, message = "diskSize max 10")
    public Integer diskSize;
}
