package cn.xej.api;

import cn.xej.api.models.*;
import cn.xej.api.common.AbstractClient;
import cn.xej.api.common.ApiSDKException;
import cn.xej.api.common.Credential;

/**
 * 统一API客户端
 */
public class ApiClient extends AbstractClient {

    private static final String DEFAULT_ENDPOINT = "localhost:8080";

    public ApiClient(Credential cred) {
        super(DEFAULT_ENDPOINT, cred);
    }

    public ApiClient(String endpoint, Credential cred) {
        super(endpoint, cred);
    }

    public CreateUserResponse createUser(CreateUserRequest request) throws ApiSDKException {
        return this.internalRequest(request, "CreateUser", CreateUserResponse.class);
    }
    public DescribeUsersResponse describeUsers(DescribeUsersRequest request) throws ApiSDKException {
        return this.internalRequest(request, "DescribeUsers", DescribeUsersResponse.class);
    }
    public DescribeInstancesResponse describeInstances(DescribeInstancesRequest request) throws ApiSDKException {
        return this.internalRequest(request, "DescribeInstances", DescribeInstancesResponse.class);
    }
    public CreateInstanceResponse createInstance(CreateInstanceRequest request) throws ApiSDKException {
        return this.internalRequest(request, "CreateInstance", CreateInstanceResponse.class);
    }

}