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

package com.nageoffer.shortlink.project.mq.producer;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * 短链接监控状态保存消息队列生产者
 * 公众号：马丁玩编程，回复：加群，添加马哥微信（备注：link）获取项目资料
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsSaveProducer {

    private final RocketMQTemplate rocketMQTemplate;

    @Value("${rocketmq.producer.topic}")
    private String statsSaveTopic;

    /**
     * 发送延迟消费短链接统计
     */
    public void send(Map<String, String> producerMap) {
        // 业务ID被设置为UUID
        String keys = UUID.randomUUID().toString();
        producerMap.put("keys", keys);
        Message<Map<String, String>> build = MessageBuilder
                .withPayload(producerMap)
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .build();
        try {
            rocketMQTemplate.asyncSend(statsSaveTopic, build, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    log.info("[消息访问统计监控] 异步消息发送结果：{}，消息ID：{}，消息Keys：{}", sendResult.getSendStatus(), sendResult.getMsgId(), keys);
                }

                @Override
                public void onException(Throwable ex) {
                    log.error("[消息访问统计监控] 异步消息发送失败，消息体：{}，消息Keys：{}", JSON.toJSONString(producerMap), keys, ex);
                }
            }, 2000L);
        } catch (Throwable ex) {
            log.error("[消息访问统计监控] 异步消息提交失败，消息体：{}，消息Keys：{}", JSON.toJSONString(producerMap), keys, ex);
            // 自定义行为...
        }
    }
}
