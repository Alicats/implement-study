package cn.xej.api.request;


import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import cn.xej.api.model.User;

@Data
@AllArgsConstructor
public class DescribeUsersResponse {
    public List<User> users;
}
