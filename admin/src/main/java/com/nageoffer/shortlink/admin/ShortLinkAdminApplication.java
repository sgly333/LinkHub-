/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.shortlink.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 短链接后管应用
 *  
 */
@SpringBootApplication
// @EnableDiscoveryClient让当前服务可以注册到注册中心，并且可以发现其他服务。
@EnableDiscoveryClient
// @EnableFeignClients 开启 Feign 客户端功能，并扫描指定包中的 Feign 接口，让 Spring 自动生成远程调用代理。
@EnableFeignClients("com.nageoffer.shortlink.admin.remote")
// 在启动类中 让 Spring 自动扫描指定包下的 MyBatis Mapper 接口，并把它们注册为 Bean。
@MapperScan("com.nageoffer.shortlink.admin.dao.mapper")

public class ShortLinkAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkAdminApplication.class, args);
    }
}
