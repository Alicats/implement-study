package cn.xej.mybatis;

import java.lang.reflect.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MapperInvocationHandler implements InvocationHandler {

    private static final String URL = "jdbc:mysql://10.89.0.11:30602/test?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "uEXsn7NZrusBIGKe";

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
     *  - 获取返回参数集合
     *  - 获取表名
     *  - 获取请求参数集合
     *
     * 2、填充SQL值
     *  - 遍历args 判断int、String类型填充值
     *
     * 3、生成对象
     *  - 通过构造方法生成
     *  - 属性填充值
     *  - 若method返回List，需要获取泛型的具体类型
     */
    
    private Object invokeSelect(Object proxy, Method method, Object[] args) {
        List<String> selectCols = getSelectCols(method);
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ");
        sb.append(String.join(",", selectCols));
        sb.append(" FROM ");
        //数据库表名
        String tableName = getTableName(method);
        sb.append(tableName);
        sb.append(" WHERE ");
        //请求参数名
        String condition = getCondition(method);
        sb.append(condition);
        String sql = sb.toString();
//        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                if (arg instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer) arg);
                }else if (arg instanceof String) {
                    preparedStatement.setString(i + 1, arg.toString());
                }
            }

            ResultSet resultSet = preparedStatement.executeQuery();

            //判断方法返回值类型是不是List
            if (method.getReturnType().isAssignableFrom(List.class)) {
                List<Object> resultList = new ArrayList<>();
                while (resultSet.next()) {
                    Object result = parseResult(resultSet, getListGenericType(method));
                    resultList.add(result);
                }
                return resultList;
            }else {
                if (resultSet.next()) {
                    return parseResult(resultSet, getListGenericType(method));
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Object parseResult(ResultSet resultSet, Class<?> classType) throws Exception {
        Constructor<?> constructor = classType.getDeclaredConstructor();
        Object result = constructor.newInstance();
        Field[] fields = classType.getDeclaredFields();
        for (Field field : fields) {
            Object column = null;
            //字段名
            String name = field.getName();
            Class<?> fieldType = field.getType();
            if (fieldType == String.class) {
                column = resultSet.getString(name);

            }else if (fieldType == int.class) {
                column = resultSet.getInt(name);
            }
            //设置可访问性（针对私有属性）
            field.setAccessible(true);
            //设置属性值
            field.set(result, column);
        }
        return result;
    }

    private String getCondition(Method method) {
        List<String> cols = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters) {
            Param param = parameter.getAnnotation(Param.class);
            if (param != null) {
                cols.add(param.name() + " = ? ");
            }
        }
        return String.join(" AND ", cols);
    }

    //这段代码的作用是 通过反射获取方法返回的 List 集合中元素的泛型类型（Class 对象）
    private Class<?> getListGenericType(Method method) {
        if (!method.getReturnType().isAssignableFrom(List.class)) {
            return method.getReturnType();
        }

        Type returnType = method.getGenericReturnType();
        if (returnType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) returnType;
            Type[] typeArguments = type.getActualTypeArguments();
            if (typeArguments.length > 0) {
                Type typeArgument = typeArguments[0];
                if (typeArgument instanceof Class) {
                    return (Class<?>) typeArgument;
                }
            }
        }
        return null;
    }

    private String getTableName(Method method) {
        Class<?> aClass = getListGenericType(method);
        Table table = aClass.getAnnotation(Table.class);
        if (table == null) {
            throw new RuntimeException("table not found");
        }
        return table.name();
    }

    private List<String> getSelectCols(Method method) {
        Class<?> aClass = getListGenericType(method);
        Field[] fields = aClass.getDeclaredFields();
        return Arrays.stream(fields).map(Field::getName).collect(Collectors.toList());
    }
}
