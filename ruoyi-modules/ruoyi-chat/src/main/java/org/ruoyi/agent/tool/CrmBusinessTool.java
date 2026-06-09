package org.ruoyi.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.service.chat.impl.agent.crm.CrmAgentBusinessService;
import org.springframework.stereotype.Component;

/**
 * High-level CRM tools exposed to agents.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrmBusinessTool {

    private final CrmAgentBusinessService crmAgentBusinessService;

    @Tool("获取 CRM 跟进记录草稿的数据结构、字段含义、必填规则和字典值。生成跟进草稿前必须先查看。")
    public String crmGetFollowRecordDraftSchema() {
        log.info("【CRM业务工具】crmGetFollowRecordDraftSchema");
        return toJson(crmAgentBusinessService.getFollowRecordDraftSchema());
    }

    @Tool("按客户编号、客户名称、客户简称或客户ID搜索 CRM 客户，返回候选客户。客户无法唯一定位时优先使用。")
    public String crmSearchCustomer(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmSearchCustomer input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.searchCustomer(customerCodeOrNameOrId));
    }

    @Tool("按客户编号、客户名称或客户ID查询 CRM 客户全景，包含客户主档和联系人。优先使用客户编号。")
    public String crmGetCustomerOverview(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmGetCustomerOverview input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.getCustomerOverview(customerCodeOrNameOrId));
    }

    @Tool("按客户编号、客户名称或客户ID查询 CRM 客户业务时间线，包含跟进记录、商机、报价、合同、回款计划。用户问客户历史、进展、往来记录时使用。")
    public String crmGetCustomerTimeline(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmGetCustomerTimeline input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.getCustomerTimeline(customerCodeOrNameOrId));
    }

    @Tool("按客户编号、客户名称或客户ID查询该客户的 CRM 联系人列表。用户问联系人、电话、邮箱、职位、决策人时使用。")
    public String crmListContactsByCustomer(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmListContactsByCustomer input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.listContactsByCustomer(customerCodeOrNameOrId));
    }

    @Tool("按客户编号、客户名称或客户ID查询该客户的 CRM 商机列表。用户问商机、项目机会、销售阶段、预计金额时使用。")
    public String crmListOpportunitiesByCustomer(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmListOpportunitiesByCustomer input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.listOpportunitiesByCustomer(customerCodeOrNameOrId));
    }

    @Tool("按客户编号/客户名称/客户ID或商机ID查询 CRM 报价列表。按商机查时传 opportunityId；否则传客户标识。")
    public String crmListQuotesByCustomerOrOpportunity(String customerCodeOrNameOrId, String opportunityId) {
        log.info("【CRM业务工具】crmListQuotesByCustomerOrOpportunity customer={}, opportunityId={}", customerCodeOrNameOrId, opportunityId);
        return toJson(crmAgentBusinessService.listQuotesByCustomerOrOpportunity(customerCodeOrNameOrId, opportunityId));
    }

    @Tool("按客户编号/客户名称/客户ID或商机ID查询 CRM 合同列表。用户问某客户有哪些合同、合同情况、合作合同等场景必须使用。按商机查时传 opportunityId；否则传客户标识。")
    public String crmListContractsByCustomerOrOpportunity(String customerCodeOrNameOrId, String opportunityId) {
        log.info("【CRM业务工具】crmListContractsByCustomerOrOpportunity customer={}, opportunityId={}", customerCodeOrNameOrId, opportunityId);
        return toJson(crmAgentBusinessService.listContractsByCustomerOrOpportunity(customerCodeOrNameOrId, opportunityId));
    }

    @Tool("按客户编号/客户名称/客户ID或合同ID查询 CRM 回款计划。按合同查时传 contractId；否则传客户标识。")
    public String crmListPaymentPlansByCustomerOrContract(String customerCodeOrNameOrId, String contractId) {
        log.info("【CRM业务工具】crmListPaymentPlansByCustomerOrContract customer={}, contractId={}", customerCodeOrNameOrId, contractId);
        return toJson(crmAgentBusinessService.listPaymentPlansByCustomerOrContract(customerCodeOrNameOrId, contractId));
    }

    @Tool("按业务单据类型和ID查询 CRM 详情。documentType 支持 customer/contact/opportunity/quote/contract/paymentPlan/followRecord 或中文类型。")
    public String crmGetBusinessDocumentDetail(String documentType, String documentId) {
        log.info("【CRM业务工具】crmGetBusinessDocumentDetail type={}, id={}", documentType, documentId);
        return toJson(crmAgentBusinessService.getBusinessDocumentDetail(documentType, documentId));
    }

    @Tool("按客户编号、客户名称或客户ID查询该客户的 CRM 合同列表。兼容旧调用；新问题优先使用 crmListContractsByCustomerOrOpportunity。")
    public String crmGetCustomerContracts(String customerCodeOrNameOrId) {
        log.info("【CRM业务工具】crmGetCustomerContracts input={}", customerCodeOrNameOrId);
        return toJson(crmAgentBusinessService.getCustomerContracts(customerCodeOrNameOrId));
    }

    @Tool("为 CRM 客户生成跟进记录草稿，不直接入库。用于用户说拜访、电话、微信、邮件沟通、商讨合作等记录类需求。")
    public String crmDraftFollowRecord(String customerCodeOrNameOrId, String content, String followMethod,
                                       String result, String followDateText, String nextFollowDateText) {
        log.info("【CRM业务工具】crmDraftFollowRecord customer={}, content={}", customerCodeOrNameOrId, content);
        return toJson(crmAgentBusinessService.draftFollowRecord(
            customerCodeOrNameOrId,
            content,
            followMethod,
            result,
            followDateText,
            nextFollowDateText
        ));
    }

    @Tool("用户明确确认后，按服务端草稿ID新增 CRM 跟进记录入库。没有用户确认时禁止调用。")
    public String crmCreateFollowRecordConfirmed(String draftId) {
        log.info("【CRM业务工具】crmCreateFollowRecordConfirmed draftId={}", draftId);
        return toJson(crmAgentBusinessService.createFollowRecordConfirmed(draftId));
    }

    private String toJson(Object value) {
        try {
            return JsonUtils.toJsonString(value);
        } catch (Exception e) {
            log.error("CRM业务工具返回值序列化失败", e);
            return "{\"success\":false,\"message\":\"序列化失败\"}";
        }
    }
}
