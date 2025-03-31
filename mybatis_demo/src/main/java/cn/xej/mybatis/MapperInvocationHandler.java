package cn.xej.mybatis;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MapperInvocationHandler implements InvocationHandler {

    private static final String URL = "jdbc:mysql://localhost:3306/mybatis_demo?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.getName().startsWith("select")) {
            return invokeSelect(proxy, method, args);
        }
        return null;
    }

    /**
     * mybatis动态代理
     *
     * 代理对象代理接口实现类逻辑
     *
     * 1、创建SQL （select id,name,age from user where id = ?）
     *
     * - 获取返回参数集合
     * - 获取表名
     * - 获取请求参数集合
     */
    private Object invokeSelect(Object proxy, Method method, Object[] args) {
        List<String> selectCols = getSelectCols(proxy,method,args)
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                User user = new User();
                user.setId(resultSet.getInt("id"));
                user.setName(resultSet.getString("name"));
                user.setAge(resultSet.getInt("age"));
                return user;
            }

        } catch (SQLException e) {

        }
        return null;


    }
}
