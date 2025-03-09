package com.xzn.shortlink.project.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * 初始化限流配置
 */
@Component
public class SentinelRuleConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("create_short-link");
        // 每秒最多只能通过5个请求
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createOrderRule.setCount(10000);
        rules.add(createOrderRule);
        FlowRuleManager.loadRules(rules);
    }
}
