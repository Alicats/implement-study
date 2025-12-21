package cn.xej.api.response;

import lombok.Data;
import java.util.List;
import cn.xej.api.model.Instance;

@Data
public class DescribeInstancesResponse {
    public List<Instance> instances;
    public Integer totalCount;
}
