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

package com.nageoffer.shortlink.admin.common.convention.result;

import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局返回对象
 *  
 */
@Data
// @Accessors(chain = true) 让 setter 方法支持链式调用（chain call
@Accessors(chain = true)
public class Result<T> implements Serializable {

    @Serial
    // 标记序列化相关成员
    // serialVersionUID 的作用 标识类的序列化版本， 保证反序列化时类的兼容性， 避免类修改导致反序列化失败。
    private static final long serialVersionUID = 5679018624309023727L;

    /**
     * 正确返回码
     */
    public static final String SUCCESS_CODE = "0";

    /**
     * 返回码
     */
    private String code;

    /**
     * 返回消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 请求ID
     */
    private String requestId;

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }
}
