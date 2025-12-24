package ${packageName};

import cn.xej.api.common.AbstractClient;
import cn.xej.api.common.ApiSDKException;
import ${packageName}.models.*;

/**
 * 统一API客户端
 */
public class ApiClient extends AbstractClient {
    public ApiClient(String baseUrl) {
        super(baseUrl);
    }

    <#list methods as method>
    public ${method.returnType} ${method.methodName}(${method.parameters}) throws ApiSDKException {
        return this.internalRequest(request, "${method.fullMapping}", ${method.returnType}.class);
    }
    </#list>
}
