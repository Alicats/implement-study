package cn.xej.mybatis;

import java.lang.reflect.Proxy;

public class MySqlSessionFactory {


    public <T> T getMapper(Class<T> mapperClass) {
        //jdk 动态代理
        //参数1：类加载器，表示用什么类加载该类
        //参数2：代理对象需要实现的接口
        //参数3：代理对象需要实现的接口的实现类
        return (T) Proxy.newProxyInstance(mapperClass.getClassLoader(), new Class[]{mapperClass}, new MapperInvocationHandler());
    }




}
