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

package com.nageoffer.shortlink.project.mq.consumer;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nageoffer.shortlink.project.dao.entity.LinkAccessStatsDO;
import com.nageoffer.shortlink.project.dao.entity.LinkStatsTodayDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGotoDO;
import com.nageoffer.shortlink.project.dao.mapper.LinkAccessStatsMapper;
import com.nageoffer.shortlink.project.dao.mapper.LinkStatsTodayMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkGotoMapper;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ShortLinkStatsFlushTask {

    private static final String STATS_FLUSH_KEY_PREFIX = "short-link:stats:flush";
    private static final String STATS_FLUSH_PROCESSING_KEY_PREFIX = STATS_FLUSH_KEY_PREFIX + ":processing";

    private final StringRedisTemplate stringRedisTemplate;
    private final ShortLinkGotoMapper shortLinkGotoMapper;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final ShortLinkMapper shortLinkMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;

    @Scheduled(fixedDelayString = "${short-link.stats.flush.interval-ms:30000}")
    public void flushStatsToDb() {
        List<String> pendingKeys = scanKeys(STATS_FLUSH_KEY_PREFIX + ":*");
        for (String pendingKey : pendingKeys) {
            if (pendingKey.startsWith(STATS_FLUSH_PROCESSING_KEY_PREFIX)) {
                continue;
            }
            String processingKey = STATS_FLUSH_PROCESSING_KEY_PREFIX + ":" + UUID.randomUUID();
            Boolean renameResult;
            try {
                renameResult = stringRedisTemplate.renameIfAbsent(pendingKey, processingKey);
            } catch (Exception ex) {
                log.debug("stats key was already moved or missing, pendingKey={}", pendingKey, ex);
                continue;
            }

            // 返回 true：成功将 pendingKey 改名为 processingKey，当前实例拿到处理权
            // 返回 false：改名未成功，当前实例不处理该 key
            if (!Boolean.TRUE.equals(renameResult)) {
                continue;
            }
            try {
                Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(processingKey);
                if (entries == null || entries.isEmpty()) {
                    stringRedisTemplate.delete(processingKey);
                    continue;
                }
                String fullShortUrl = valueOf(entries.get("fullShortUrl"));
                if (fullShortUrl == null || fullShortUrl.isEmpty()) {
                    stringRedisTemplate.delete(processingKey);
                    continue;
                }
                long pv = toLong(entries.get("pv"));
                long uv = toLong(entries.get("uv"));
                long uip = toLong(entries.get("uip"));
                if (pv == 0L && uv == 0L && uip == 0L) {
                    stringRedisTemplate.delete(processingKey);
                    continue;
                }
                int hour = (int) toLong(entries.get("hour"));
                int weekday = (int) toLong(entries.get("weekday"));
                Date date = DateUtil.parseDate(valueOf(entries.get("date")));

                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                if (shortLinkGotoDO == null) {
                    stringRedisTemplate.delete(processingKey);
                    continue;
                }
                String gid = shortLinkGotoDO.getGid();
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .pv((int) pv)
                        .uv((int) uv)
                        .uip((int) uip)
                        .hour(hour)
                        .weekday(weekday)
                        .fullShortUrl(fullShortUrl)
                        .date(date)
                        .build();
                linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
                shortLinkMapper.incrementStats(gid, fullShortUrl, (int) pv, (int) uv, (int) uip);
                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
                        .todayPv((int) pv)
                        .todayUv((int) uv)
                        .todayUip((int) uip)
                        .fullShortUrl(fullShortUrl)
                        .date(date)
                        .build();
                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
                stringRedisTemplate.delete(processingKey);
            } catch (Exception ex) {
                log.error("[消息访问统计监控] 刷库失败，key={}", pendingKey, ex);
                try {
                    stringRedisTemplate.renameIfAbsent(processingKey, pendingKey);
                } catch (Exception restoreEx) {
                    log.error("[消息访问统计监控] 恢复Redis统计key异常，processingKey={}, pendingKey={}", processingKey, pendingKey, restoreEx);
                }
            }
        }
    }

    private List<String> scanKeys(String pattern) {
        try {
            Set<String> keys = stringRedisTemplate.keys(pattern);
            return keys == null ? new ArrayList<>() : new ArrayList<>(keys);
        } catch (Exception ex) {
            log.error("[消息访问统计监控] 扫描Redis统计key异常", ex);
            return new ArrayList<>();
        }
    }

    private String valueOf(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }
}
