package cn.xej.api.request;

import javax.validation.constraints.NotEmpty;
import java.util.Set;

public class DescribeUsersRequest {
    @NotEmpty(message = "userIds not empty")
    public Set<String> userIds;
    public Integer pageSize = 10;
    public Integer pageNum = 1;
}
