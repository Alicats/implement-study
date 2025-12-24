package cn.xej.api;

import com.tencentcloudapi.cvm.v20170312.CvmClient;
import com.tencentcloudapi.cvm.v20170312.models.DescribeInstancesRequest;
import com.tencentcloudapi.cvm.v20170312.models.DescribeInstancesResponse;
import com.tencentcloudapi.common.Credential;


public class Test {
    public static void main(String[] args) {
        Credential cred = new Credential(System.getenv("TENCENTCLOUD_SECRET_ID"), System.getenv("TENCENTCLOUD_SECRET_KEY"));
        CvmClient client = new CvmClient(cred, "ap-shanghai");
        DescribeInstancesRequest req = new DescribeInstancesRequest();
        String[] instanceIds = {"ins-alicat"};
        req.setInstanceIds(instanceIds);
        try {
            DescribeInstancesResponse resp = client.DescribeInstances(req);
            System.out.println(DescribeInstancesResponse.toJsonString(resp));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
