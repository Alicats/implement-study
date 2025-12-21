package cn.xej.api.response;


import lombok.Data;
import java.util.List;
import cn.xej.api.model.User;

@Data
public class DescribeUsersResponse {
    public List<User> userSet;
    public Integer    totalCount;

}
