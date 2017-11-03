/*******************************************************************************
 *
 * ==============================================================================
 *
 * Copyright (c) 2001-2016 Primeton Technologies, Ltd.
 * All rights reserved.
 * 
 * Created on 2016年12月20日 下午3:44:09
 *******************************************************************************/

package com.primeton.zipkin.service1;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Api("service的API接口")
@RestController
@RequestMapping("/service1")
public class ZipkinBraveController {

    @Autowired
    private OkHttpClient client;

    private static final Logger LOG = LoggerFactory.getLogger(ZipkinBraveController.class);

    @Autowired
    private RestTemplate restTemplate;

    @ApiOperation("trace第一步")
    @RequestMapping("/test")
    public String service1() throws Exception {
        Thread.sleep(100);
        LOG.info("test");
        Request request = new Request.Builder().url("http://localhost:8082/test").build();
        Response response = client.newCall(request).execute();
        return response.body().string();
    }

    @RequestMapping("/hi")
    public String callHome() {
        LOG.info("calling trace service1 ");
        return restTemplate.getForObject("http://localhost:8081/service1/test", String.class);
    }

    @RequestMapping("/api")
    public ResponseEntity<String> printDate(@RequestHeader(value = "user-name", required = false) String username) {
        String result;
        if (username != null) {
            result = new Date().toString() + " " + username;
        } else {
            result = new Date().toString();
        }
        return new ResponseEntity<String>(result, HttpStatus.GATEWAY_TIMEOUT);
    }

    @RequestMapping("/start")
    public ResponseEntity<String> callBackend() {
        String result = restTemplate.getForObject("http://localhost:8081/service1/api", String.class);
        return new ResponseEntity<String>(result, HttpStatus.GATEWAY_TIMEOUT);
    }
}
