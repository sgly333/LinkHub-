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

package com.nageoffer.shortlink.project.cache;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.GOTO_IS_NULL_SHORT_LINK_KEY;
import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;
import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.GOTO_CACHE_DELETE_RETRY_QUEUE_KEY;

/**
 * 跳转缓存 Redis 删除失败时入队，定时批量重试删除；成功后通知本地与其它实例失效跳转缓存。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GotoRedisCacheDeleteRetrySupport {

    private static final int MAX_DEQUEUE_PER_TICK = 200;

    private final StringRedisTemplate stringRedisTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 在定时任务中连续删除失败达到此次数后丢弃（首次入队记为 0，每失败一次 +1，达到上限则不再入队）。
     */
    @Value("${short-link.cache.goto-delete-retry-max-attempts:3}")
    private int maxScheduledAttempts;

    /**
     * 尝试删除 Redis 中的跳转缓存 key；若调用异常则写入重试队列。
     *
     * @return true 表示本次删除调用未抛异常（含 key 不存在）
     */
    public boolean deleteOrEnqueue(String redisKey) {
        try {
            stringRedisTemplate.delete(redisKey);
            return true;
        } catch (Exception ex) {
            log.warn("[goto-cache] Redis delete failed, will retry later, key={}", redisKey, ex);
            enqueuePayload(new QueuePayload(0, redisKey));
            return false;
        }
    }

    private void enqueuePayload(QueuePayload payload) {
        try {
            stringRedisTemplate.opsForList().rightPush(GOTO_CACHE_DELETE_RETRY_QUEUE_KEY, JSON.toJSONString(payload));
        } catch (Exception ex) {
            log.error("[goto-cache] enqueue delete-retry failed, key={}", payload.k, ex);
        }
    }

    @Scheduled(fixedDelayString = "${short-link.cache.goto-delete-retry-interval-ms:30000}")
    // 单位ms
    public void processRetryQueue() {
        for (int i = 0; i < MAX_DEQUEUE_PER_TICK; i++) {
            String raw;
            try {
                raw = stringRedisTemplate.opsForList().leftPop(GOTO_CACHE_DELETE_RETRY_QUEUE_KEY);
            } catch (Exception ex) {
                log.warn("[goto-cache] retry queue leftPop failed", ex);
                return;
            }
            if (raw == null) {
                return;
            }
            QueuePayload payload = parseQueuePayload(raw);
            if (payload == null) {
                continue;
            }
            try {
                stringRedisTemplate.delete(payload.k);
            } catch (Exception ex) {
                int cap = Math.max(1, maxScheduledAttempts);
                int nextFailures = payload.s + 1;
                if (nextFailures >= cap) {
                    log.warn(
                            "[goto-cache] retry delete abandoned after {} failures (max={}), key={}",
                            nextFailures,
                            cap,
                            payload.k,
                            ex);
                    return;
                }
                log.warn(
                        "[goto-cache] retry delete still failed ({}/{}), re-enqueue, key={}",
                        nextFailures,
                        cap,
                        payload.k,
                        ex);
                enqueuePayload(new QueuePayload(nextFailures, payload.k));
                return;
            }
            String fullShortUrl = parseFullShortUrl(payload.k);
            if (fullShortUrl != null) {
                eventPublisher.publishEvent(new GotoRedisCacheRetrySuccessEvent(fullShortUrl));
            }
        }
    }

    /**
     * 队列元素为 JSON：{@code {"s":n,"k":"完整 Redis key"}}，解析失败则丢弃该条（已出队）。
     */
    private QueuePayload parseQueuePayload(String raw) {
        try {
            QueuePayload p = JSON.parseObject(raw.trim(), QueuePayload.class);
            if (p != null && p.k != null && !p.k.isEmpty()) {
                return p;
            }
        } catch (Exception ex) {
            log.warn("[goto-cache] invalid retry queue entry (expected JSON), raw={}", raw, ex);
            return null;
        }
        log.warn("[goto-cache] invalid retry queue entry (missing k), raw={}", raw);
        return null;
    }

    static final class QueuePayload {
        /** 已进入队列后，在定时任务里累计的删除失败次数（不含本次将要执行的那次）。 */
        public int s;
        public String k;

        QueuePayload() {}

        QueuePayload(int s, String k) {
            this.s = s;
            this.k = k;
        }
    }

    static String parseFullShortUrl(String redisKey) {
        String gotoPrefix = String.format(GOTO_SHORT_LINK_KEY, "");
        if (redisKey.startsWith(gotoPrefix)) {
            return redisKey.substring(gotoPrefix.length());
        }
        String isNullPrefix = String.format(GOTO_IS_NULL_SHORT_LINK_KEY, "");
        if (redisKey.startsWith(isNullPrefix)) {
            return redisKey.substring(isNullPrefix.length());
        }
        return null;
    }
}
