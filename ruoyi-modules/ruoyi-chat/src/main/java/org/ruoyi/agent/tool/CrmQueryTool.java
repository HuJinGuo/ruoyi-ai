package org.ruoyi.agent.tool;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmPaymentPlanMapper;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;
import org.springframework.stereotype.Component;

/**
 * CRM query tools for agentic sub-agents.
 */
@Slf4j
@Component
public class CrmQueryTool {

    private static final String LIMIT_20 = "limit 20";

    @Tool("按合同ID查询 CRM 合同")
    public String crmContractQuery(String contractId) {
        log.info("【CRM工具调用】crmContractQuery contractId={}", contractId);
        return toJson(SpringUtils.getBean(CrmContractMapper.class).selectById(parseId(contractId, "contractId")));
    }

    @Tool("按合同名称模糊查询 CRM 合同")
    public String crmContractQueryByName(String contractName) {
        log.info("【CRM工具调用】crmContractQueryByName contractName={}", contractName);
        return toJson(SpringUtils.getBean(CrmContractMapper.class)
            .selectList(new LambdaQueryWrapper<CrmContract>()
                .like(CrmContract::getName, contractName)
                .orderByDesc(CrmContract::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按客户ID查询 CRM 客户")
    public String crmCustomerQuery(String customerId) {
        log.info("【CRM工具调用】crmCustomerQuery customerId={}", customerId);
        return toJson(SpringUtils.getBean(CrmCustomerMapper.class).selectById(parseId(customerId, "customerId")));
    }

    @Tool("按客户名称、简称或编码模糊查询 CRM 客户")
    public String crmCustomerQueryByName(String customerName) {
        log.info("【CRM工具调用】crmCustomerQueryByName customerName={}", customerName);
        return toJson(SpringUtils.getBean(CrmCustomerMapper.class)
            .selectList(new LambdaQueryWrapper<CrmCustomer>()
                .and(wrapper -> wrapper
                    .like(CrmCustomer::getName, customerName)
                    .or()
                    .like(CrmCustomer::getShortName, customerName)
                    .or()
                    .like(CrmCustomer::getCode, customerName))
                .orderByDesc(CrmCustomer::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按客户ID查询 CRM 客户联系人")
    public String crmContactQueryByCustomerId(String customerId) {
        log.info("【CRM工具调用】crmContactQueryByCustomerId customerId={}", customerId);
        Long id = parseId(customerId, "customerId");
        return toJson(SpringUtils.getBean(CrmContactMapper.class)
            .selectList(new LambdaQueryWrapper<CrmContact>()
                .eq(CrmContact::getCustomerId, id)
                .orderByDesc(CrmContact::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按联系人姓名、手机号或邮箱模糊查询 CRM 联系人")
    public String crmContactQueryByName(String contactName) {
        log.info("【CRM工具调用】crmContactQueryByName contactName={}", contactName);
        return toJson(SpringUtils.getBean(CrmContactMapper.class)
            .selectList(new LambdaQueryWrapper<CrmContact>()
                .and(wrapper -> wrapper
                    .like(CrmContact::getName, contactName)
                    .or()
                    .like(CrmContact::getPhone, contactName)
                    .or()
                    .like(CrmContact::getEmail, contactName))
                .orderByDesc(CrmContact::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按商机ID查询 CRM 商机")
    public String crmOpportunityQuery(String opportunityId) {
        log.info("【CRM工具调用】crmOpportunityQuery opportunityId={}", opportunityId);
        return toJson(SpringUtils.getBean(CrmOpportunityMapper.class).selectById(parseId(opportunityId, "opportunityId")));
    }

    @Tool("按商机名称模糊查询 CRM 商机")
    public String crmOpportunityQueryByName(String opportunityName) {
        log.info("【CRM工具调用】crmOpportunityQueryByName opportunityName={}", opportunityName);
        return toJson(SpringUtils.getBean(CrmOpportunityMapper.class)
            .selectList(new LambdaQueryWrapper<CrmOpportunity>()
                .like(CrmOpportunity::getName, opportunityName)
                .orderByDesc(CrmOpportunity::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按客户ID查询 CRM 报价")
    public String crmQuoteQueryByCustomerId(String customerId) {
        log.info("【CRM工具调用】crmQuoteQueryByCustomerId customerId={}", customerId);
        Long id = parseId(customerId, "customerId");
        return toJson(SpringUtils.getBean(CrmQuoteMapper.class)
            .selectList(new LambdaQueryWrapper<CrmQuote>()
                .eq(CrmQuote::getCustomerId, id)
                .orderByDesc(CrmQuote::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按商机ID查询 CRM 报价")
    public String crmQuoteQueryByOpportunityId(String opportunityId) {
        log.info("【CRM工具调用】crmQuoteQueryByOpportunityId opportunityId={}", opportunityId);
        Long id = parseId(opportunityId, "opportunityId");
        return toJson(SpringUtils.getBean(CrmQuoteMapper.class)
            .selectList(new LambdaQueryWrapper<CrmQuote>()
                .eq(CrmQuote::getOpportunityId, id)
                .orderByDesc(CrmQuote::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按客户ID查询 CRM 回款计划")
    public String crmPaymentPlanQueryByCustomerId(String customerId) {
        log.info("【CRM工具调用】crmPaymentPlanQueryByCustomerId customerId={}", customerId);
        Long id = parseId(customerId, "customerId");
        return toJson(SpringUtils.getBean(CrmPaymentPlanMapper.class)
            .selectList(new LambdaQueryWrapper<CrmPaymentPlan>()
                .eq(CrmPaymentPlan::getCustomerId, id)
                .orderByDesc(CrmPaymentPlan::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按合同ID查询 CRM 回款计划")
    public String crmPaymentPlanQueryByContractId(String contractId) {
        log.info("【CRM工具调用】crmPaymentPlanQueryByContractId contractId={}", contractId);
        Long id = parseId(contractId, "contractId");
        return toJson(SpringUtils.getBean(CrmPaymentPlanMapper.class)
            .selectList(new LambdaQueryWrapper<CrmPaymentPlan>()
                .eq(CrmPaymentPlan::getContractId, id)
                .orderByDesc(CrmPaymentPlan::getCreateTime)
                .last(LIMIT_20)));
    }

    @Tool("按商机ID查询 CRM 回款计划")
    public String crmPaymentPlanQueryByOpportunityId(String opportunityId) {
        log.info("【CRM工具调用】crmPaymentPlanQueryByOpportunityId opportunityId={}", opportunityId);
        Long id = parseId(opportunityId, "opportunityId");
        return toJson(SpringUtils.getBean(CrmPaymentPlanMapper.class)
            .selectList(new LambdaQueryWrapper<CrmPaymentPlan>()
                .eq(CrmPaymentPlan::getOpportunityId, id)
                .orderByDesc(CrmPaymentPlan::getCreateTime)
                .last(LIMIT_20)));
    }

    private Long parseId(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + "格式不正确: " + value, e);
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return "{\"data\":null,\"message\":\"未查询到数据\"}";
        }
        try {
            String json = JsonUtils.toJsonString(value);
            return json == null || json.isBlank()
                ? "{\"data\":null,\"message\":\"未查询到数据\"}"
                : json;
        } catch (Exception e) {
            log.error("CRM工具返回值序列化失败", e);
            return "Error: " + e.getMessage();
        }
    }
}
