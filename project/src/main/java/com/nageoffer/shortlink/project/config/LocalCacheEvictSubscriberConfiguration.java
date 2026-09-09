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

package com.nageoffer.shortlink.project.config;

import com.nageoffer.shortlink.project.service.impl.ShortLinkServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.SHORT_LINK_LOCAL_CACHE_EVICT_CHANNEL;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class LocalCacheEvictSubscriberConfiguration {

    private final ShortLinkServiceImpl shortLinkService;

    @Bean
    public RedisMessageListenerContainer localCacheEvictListenerContainer(RedisConnectionFactory redisConnectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(localCacheEvictListener(), new ChannelTopic(SHORT_LINK_LOCAL_CACHE_EVICT_CHANNEL));
        return container;
    }

    @Bean
    public MessageListener localCacheEvictListener() {
        return (Message message, byte[] pattern) -> {
            String fullShortUrl = new String(message.getBody(), StandardCharsets.UTF_8);
            shortLinkService.evictLocalCache(fullShortUrl);
            log.debug("[短链接本地缓存] 接收失效广播并清理缓存: {}", fullShortUrl);
        };
    }
}
