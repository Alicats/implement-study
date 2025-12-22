package cn.xej.api.controller.api;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cn.xej.api.model.Instance;

import javax.validation.Valid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.xej.api.request.CreateInstanceRequest;
import cn.xej.api.response.CreateInstanceResponse;
import cn.xej.api.request.DescribeInstancesRequest;
import cn.xej.api.response.DescribeInstancesResponse;

@Slf4j
@RestController
@RequestMapping
public class InstanceApiController {

    @PostMapping("CreateInstance")
    public CreateInstanceResponse createInstance(@Valid @RequestBody CreateInstanceRequest request) {
        log.info("api create instance request:{}", JSON.toJSONString(request));
        int i = 1 / 0;
//        throw new RuntimeException("fail");
        return new CreateInstanceResponse("alicat-123");
    }

     @PostMapping("DescribeInstances")
     public DescribeInstancesResponse describeInstances(@Valid @RequestBody DescribeInstancesRequest request) {
        log.info("api describe instances request:{}", JSON.toJSONString(request));
        List<Instance> list = new ArrayList<>(); 
        for (String instanceId : request.instanceIds) {
            Instance instance = new Instance();
            instance.setUuid(instanceId);
            instance.setLabelName(UUID.randomUUID().toString());
            instance.setInstanceType("MHO");
            instance.setStatus("RUNNING");
            instance.setIp("10.10.10.10");
            instance.setCreateTime(new java.util.Date().toString());
            list.add(instance);
        }
        DescribeInstancesResponse response = new DescribeInstancesResponse();
        response.setInstances(list);
        response.setTotalCount(1);
        return response;
     }

}
