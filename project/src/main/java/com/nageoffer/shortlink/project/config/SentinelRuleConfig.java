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

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 初始化限流配置
 *
 */
@Component
// implements InitializingBean
// Spring 提供的 初始化回调接口 Bean 创建完成后会自动执行：afterPropertiesSet()
public class SentinelRuleConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        // 设置资源的名称  这个资源就是短链接的create方法
        createOrderRule.setResource("create_short-link");
        // 按照 QPS 限流
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        // 设置 QPS = 1 也就是每秒最多 1 次请求
        createOrderRule.setCount(10000);
        rules.add(createOrderRule);
        FlowRuleManager.loadRules(rules);
    }
}
