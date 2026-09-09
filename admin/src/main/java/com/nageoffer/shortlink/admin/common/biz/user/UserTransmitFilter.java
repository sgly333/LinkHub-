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

package com.nageoffer.shortlink.admin.common.biz.user;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * 用户信息传输过滤器
 *
 */
// @RequiredArgsConstructor 是 Lombok 提供的注解，作用是： 自动生成一个构造方法，参数是类中所有 final 字段和 @NonNull 字段。
@RequiredArgsConstructor
// Filter（过滤器） 会在 每个 HTTP 请求进入你的 Web 应用时执行。也就是说，它处在 Controller 之前、请求刚进入服务器的时候。
// 只要继承Filter 如何实现过滤器的注册 既可以在请求达到controller层之前拦截请求  这个过滤器在UserConfiguration被注册
public class UserTransmitFilter implements Filter {

    // @SneakyThrows在方法中自动处理受检异常（checked exception），不需要显式写 try-catch 或 throws
    @SneakyThrows
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String username = httpServletRequest.getHeader("username");
        if (StrUtil.isNotBlank(username)) {
            String userId = httpServletRequest.getHeader("userId");
            String realName = httpServletRequest.getHeader("realName");
            UserInfoDTO userInfoDTO = new UserInfoDTO(userId, username, realName);
            UserContext.setUser(userInfoDTO);
        }
        try {
            // try filterChain.doFilter 的作用 是继续执行后面的过滤器或Controller
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            UserContext.removeUser();
        }
    }
}