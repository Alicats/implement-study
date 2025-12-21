package cn.xej.api.request;

import java.util.Set;
import javax.validation.constraints.NotEmpty;

public class DescribeInstancesRequest {
    @NotEmpty(message = "instanceIds not empty")
    public Set<String> instanceIds;
    public String labelName;
    public Integer pageSize = 10;
    public Integer pageNum = 1;

}
