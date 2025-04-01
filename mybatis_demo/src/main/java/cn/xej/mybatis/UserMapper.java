package cn.xej.mybatis;

import java.util.List;

public interface UserMapper {

    User selectById(@Param(name = "id") int id);

    User selectByNameAndAge(@Param(name = "name") String name, @Param(name = "age") int age);

    List<User> selectByName(@Param(name = "name") String name);
}
