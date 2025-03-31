package cn.xej;

import cn.xej.mybatis.MySqlSessionFactory;
import cn.xej.mybatis.User;
import cn.xej.mybatis.UserMapper;

import java.sql.*;

public class Main {

    // 数据库连接信息
    private static final String URL = "jdbc:mysql://localhost:3306/mybatis_demo?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        MySqlSessionFactory sql = new MySqlSessionFactory();
        UserMapper userMapper = sql.getMapper(UserMapper.class);
        User user = userMapper.getUserById(1);
        System.out.println("*************************");
        System.out.println(user);
        System.out.println(userMapper.toString());
        System.out.println(userMapper.hashCode());
//        User user = jdbcSelectId(2);
//        System.out.println(user);
    }



    public static void DynamicProxy() {
//        SqlSessionFactory sqlSessionFactory =
//                new SqlSessionFactoryBuilder().build(inputStream);

        // 3. 获取 SqlSession
//        sqlSession = sqlSessionFactory.openSession();

        // 4. 获取 Mapper 接口  动态代理
//        UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

        // 5. 执行查询
//        User user = userMapper.selectUserById(1L);
//        System.out.println("查询结果: " + user);
    }

    @SuppressWarnings("all")
    public static User jdbcSelectId(int id) {
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

//    public static void main(String[] args) {
//        Connection connection = null;
//        Statement statement = null;
//        ResultSet resultSet = null;
//
//        try {
//
//            // 2. 建立连接
//            connection = DriverManager.getConnection(URL, USER, PASSWORD);
//
//            // 3. 创建 Statement 对象
//            statement = connection.createStatement();
//
//            // 4. 执行 SQL 查询
//            String sql = "SELECT * FROM user WHERE id = 1";
//            resultSet = statement.executeQuery(sql);
//
//            // 5. 处理结果集
//            while (resultSet.next()) {
//                int id = resultSet.getInt("id");
//                String name = resultSet.getString("name");
//                int age = resultSet.getInt("age");
//                User user = new User();
//                user.setId(id);
//                user.setName(name);
//                user.setAge(age);
//                System.out.println(user);
//            }
//
//        } catch (Exception e) {
//            System.err.println("JDBC 驱动未找到！");
//            e.printStackTrace();
//        } finally {
//            // 6. 关闭资源（倒序关闭）
//            try {
//                if (resultSet != null) resultSet.close();
//                if (statement != null) statement.close();
//                if (connection != null) connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//        }
//    }
}