import cn.xej.api.ApiClient;
import cn.xej.api.models.CreateInstanceRequest;
import cn.xej.api.models.CreateInstanceResponse;

import java.io.IOException;

public class Main {


    public static void main(String[] args) throws IOException {
//        Credential cred = new Credential(System.getenv("TENCENTCLOUD_SECRET_ID"), System.getenv("TENCENTCLOUD_SECRET_KEY"));
//        CvmClient client = new CvmClient(cred, "ap-shanghai");
//﻿
//        DescribeInstancesRequest req = new DescribeInstancesRequest();
//        DescribeInstancesResponse resp = client.DescribeInstances(req);
//﻿
//        System.out.println(DescribeInstancesResponse.toJsonString(resp));

        ApiClient client = new ApiClient("http://localhost:8080");
        CreateInstanceRequest req = new CreateInstanceRequest();
        req.setInstanceTypeId("xx");
        req.setIpv4("10.0.0.1");
        req.setLabelName("alicat-123");
        req.setPassword("123456");
        try {
            CreateInstanceResponse res = client.createInstance(req);
            System.out.println(res);
        } catch (IOException e) {
            System.out.println("最终调用失败，总共尝试了 " + client.getExceptionCount() + " 次");
            throw new RuntimeException(e);
        }
//        Set<String> userIds = new HashSet<>();
//        userIds.add("123");
//        DescribeUsersRequest req = new DescribeUsersRequest();
//        req.setUserIds(userIds);
//        try {
//            DescribeUsersResponse res = client.describeUsers(req);
//            System.out.println(res);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        // CreateUserRequest req = new CreateUserRequest();
        // req.setName("张三");
        // req.setEmail("20320@qq.com");
        // try {
        //     CreateUserResponse res = client.createUser(req);
        //     System.out.println(res.getUserId());
        // } catch (Exception e) {
        //     throw new RuntimeException(e);
        // }


//        DescribeInstancesRequest req = new DescribeInstancesRequest();
//        Set<String> instanceIds = new HashSet<>();
//        instanceIds.add("ins-123");
//        req.setInstanceIds(instanceIds);
//        try {
//            DescribeInstancesResponse res = client.describeInstances(req);
//            System.out.println(res);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

//        CreateUserRequest req = new CreateUserRequest();
//        req.setName("周宏伟");
//        req.setEmail("20320@qq.com");
//        CreateUserResponse res = client.createUser(req);
//        System.out.println(res);


    }

}
