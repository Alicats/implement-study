package cn.xej.api;

import cn.xej.api.models.*;
import cn.xej.api.common.AbstractClient;
import cn.xej.api.common.ApiSDKException;

/**
 * 统一API客户端
 */
public class ApiClient extends AbstractClient {
    public ApiClient(String baseUrl) {
        super(baseUrl);
    }

    public CreateInstanceResponse createInstance(CreateInstanceRequest req) throws ApiSDKException {
        return this.internalRequest(req, "CreateInstance", CreateInstanceResponse.class);
    }

}