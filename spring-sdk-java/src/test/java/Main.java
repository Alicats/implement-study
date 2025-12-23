import cn.xej.api.ApiClient;
import cn.xej.api.common.ApiSDKException;
import cn.xej.api.models.CreateInstanceRequest;
import cn.xej.api.models.CreateInstanceResponse;

public class Main {


    public static void main(String[] args) {

        ApiClient client = new ApiClient("http://localhost:8080");
        CreateInstanceRequest req = new CreateInstanceRequest();
        req.setInstanceTypeId("xx");
        req.setIpv4("10.0.0.1");
        req.setLabelName("alicat-123");
        req.setBandwidth(1001);
        req.setPassword("123456");
        try {
            CreateInstanceResponse res = client.createInstance(req);
            System.out.println(res);
        } catch (ApiSDKException e) {
            System.out.println(e);
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
