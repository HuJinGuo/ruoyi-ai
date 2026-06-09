package org.ruoyi.service.chat.impl.agent.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmFollowRecord;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmPaymentPlan;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmFollowRecordMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmPaymentPlanMapper;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.time.Duration;

/**
 * High-level CRM operations for agents.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrmAgentBusinessService {

    private static final String LIMIT_10 = "limit 10";

    private static final String FOLLOW_DRAFT_KEY_PREFIX = "agent:crm:follow:draft:";

    private static final Duration FOLLOW_DRAFT_TTL = Duration.ofMinutes(30);

    private final CrmCustomerMapper customerMapper;
    private final CrmContactMapper contactMapper;
    private final CrmContractMapper contractMapper;
    private final CrmFollowRecordMapper followRecordMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CrmQuoteMapper quoteMapper;
    private final CrmPaymentPlanMapper paymentPlanMapper;

    public Map<String, Object> getFollowRecordDraftSchema() {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("customerCodeOrNameOrId", Map.of(
            "required", true,
            "description", "客户编号、客户名称、简称或客户ID。优先使用客户编号。"
        ));
        fields.put("followDateText", Map.of(
            "required", true,
            "description", "跟进日期。可传 今天/昨天/明天/yyyy-MM-dd；用户未说明时传 今天。"
        ));
        fields.put("followMethod", Map.of(
            "required", true,
            "dictType", "crm_follow_method",
            "dict", Map.of(
                "phone", "电话",
                "wechat", "微信",
                "email", "邮件",
                "onsite", "现场/拜访",
                "video", "视频"
            ),
            "description", "只能传字典值。拜访/现场拜访使用 onsite。"
        ));
        fields.put("content", Map.of(
            "required", true,
            "description", "由模型根据用户原话生成的跟进内容，要求客观简洁，保留客户、沟通主题和关键结论。"
        ));
        fields.put("result", Map.of(
            "required", true,
            "dictType", "crm_follow_result",
            "dict", Map.of(
                "continue", "继续推进",
                "waiting", "等待反馈",
                "paused", "暂停",
                "failed", "失败"
            ),
            "description", "只能传字典值。用户未说明负向/暂停/等待时，默认 continue。"
        ));
        fields.put("nextFollowDateText", Map.of(
            "required", false,
            "description", "下次跟进日期。用户未说明时留空，不要传未定、待定、未知。"
        ));

        return result(true, "crm_follow_record_schema", "CRM跟进记录草稿结构", Map.of(
            "table", "crm_follow_record",
            "writePolicy", "模型根据 schema 生成草稿字段；工具只校验和暂存；用户确认后才入库。",
            "fields", fields
        ));
    }

    public Map<String, Object> getCustomerOverview(String customerCodeOrNameOrId) {
        CrmCustomer customer = resolveSingleCustomer(customerCodeOrNameOrId);
        if (customer == null) {
            return result(false, "customer_overview", "未找到唯一客户，请提供客户编号或更精确名称", Map.of(
                "input", customerCodeOrNameOrId,
                "candidates", searchCustomers(customerCodeOrNameOrId)
            ));
        }

        Long customerId = customer.getCustomerId();
        List<CrmContact> contacts = listContacts(customerId);
        List<CrmOpportunity> opportunities = listOpportunities(customerId);
        List<CrmQuote> quotes = listQuotes(customerId, null);
        List<CrmContract> contracts = listContracts(customerId, null);
        List<CrmPaymentPlan> paymentPlans = listPaymentPlans(customerId, null);
        List<CrmFollowRecord> followRecords = listFollowRecords(customerId);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer", customer);
        data.put("contacts", contacts);
        data.put("opportunities", opportunities);
        data.put("quotes", quotes);
        data.put("contracts", contracts);
        data.put("paymentPlans", paymentPlans);
        data.put("followRecords", followRecords);
        data.put("summary", Map.of(
            "contactCount", contacts.size(),
            "opportunityCount", opportunities.size(),
            "quoteCount", quotes.size(),
            "contractCount", contracts.size(),
            "paymentPlanCount", paymentPlans.size(),
            "followRecordCount", followRecords.size()
        ));
        data.put("contactMessage", contacts.isEmpty() ? "客户存在，但未维护联系人" : "查询到联系人");
        return result(true, "customer_overview", "查询成功", data);
    }

    public Map<String, Object> searchCustomer(String customerCodeOrNameOrId) {
        Long id = tryParseId(StringUtils.isBlank(customerCodeOrNameOrId) ? "" : customerCodeOrNameOrId.trim());
        List<CrmCustomer> customers;
        if (id != null) {
            CrmCustomer customer = customerMapper.selectById(id);
            customers = customer == null ? List.of() : List.of(customer);
        } else {
            customers = searchCustomers(customerCodeOrNameOrId);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("input", customerCodeOrNameOrId);
        data.put("customers", customers);
        data.put("count", customers.size());
        data.put("unique", customers.size() == 1);
        return result(!customers.isEmpty(), "customer_search", customers.isEmpty() ? "未找到客户" : "查询到客户候选", data);
    }

    public Map<String, Object> getCustomerContracts(String customerCodeOrNameOrId) {
        return listContractsByCustomerOrOpportunity(customerCodeOrNameOrId, null);
    }

    public Map<String, Object> getCustomerTimeline(String customerCodeOrNameOrId) {
        CrmCustomer customer = resolveSingleCustomer(customerCodeOrNameOrId);
        if (customer == null) {
            return result(false, "customer_timeline", "未找到唯一客户，无法查询客户时间线", Map.of(
                "input", customerCodeOrNameOrId,
                "candidates", searchCustomers(customerCodeOrNameOrId),
                "needUserSelection", true
            ));
        }

        List<Map<String, Object>> timeline = new ArrayList<>();
        listFollowRecords(customer.getCustomerId()).forEach(record -> timeline.add(timelineItem("followRecord", record.getFollowTime(), record)));
        listOpportunities(customer.getCustomerId()).forEach(opportunity -> timeline.add(timelineItem("opportunity", opportunity.getCreateTime(), opportunity)));
        listQuotes(customer.getCustomerId(), null).forEach(quote -> timeline.add(timelineItem("quote", quote.getCreateTime(), quote)));
        listContracts(customer.getCustomerId(), null).forEach(contract -> timeline.add(timelineItem("contract",
            contract.getSignedDate() != null ? contract.getSignedDate() : contract.getCreateTime(), contract)));
        listPaymentPlans(customer.getCustomerId(), null).forEach(plan -> timeline.add(timelineItem("paymentPlan",
            plan.getPlannedDate() != null ? plan.getPlannedDate() : plan.getCreateTime(), plan)));
        timeline.sort(Comparator.comparing(item -> (Date) item.get("time"), Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer", customer);
        data.put("timeline", timeline);
        data.put("count", timeline.size());
        return result(true, "customer_timeline", timeline.isEmpty() ? "客户存在，但未查询到业务时间线" : "查询成功", data);
    }

    public Map<String, Object> listContactsByCustomer(String customerCodeOrNameOrId) {
        CrmCustomer customer = resolveSingleCustomer(customerCodeOrNameOrId);
        if (customer == null) {
            return customerNotUnique("customer_contacts", "未找到唯一客户，无法查询联系人", customerCodeOrNameOrId);
        }
        List<CrmContact> contacts = listContacts(customer.getCustomerId());
        return customerListResult("customer_contacts", customer, "contacts", contacts,
            contacts.isEmpty() ? "客户存在，但未维护联系人" : "查询成功");
    }

    public Map<String, Object> listOpportunitiesByCustomer(String customerCodeOrNameOrId) {
        CrmCustomer customer = resolveSingleCustomer(customerCodeOrNameOrId);
        if (customer == null) {
            return customerNotUnique("customer_opportunities", "未找到唯一客户，无法查询商机", customerCodeOrNameOrId);
        }
        List<CrmOpportunity> opportunities = listOpportunities(customer.getCustomerId());
        return customerListResult("customer_opportunities", customer, "opportunities", opportunities,
            opportunities.isEmpty() ? "客户存在，但未查询到商机" : "查询成功");
    }

    public Map<String, Object> listQuotesByCustomerOrOpportunity(String customerCodeOrNameOrId, String opportunityId) {
        Long parsedOpportunityId = tryParseId(StringUtils.isBlank(opportunityId) ? "" : opportunityId.trim());
        CrmCustomer customer = null;
        Long customerId = null;
        if (parsedOpportunityId == null) {
            customer = resolveSingleCustomer(customerCodeOrNameOrId);
            if (customer == null) {
                return customerNotUnique("customer_quotes", "未找到唯一客户，无法查询报价", customerCodeOrNameOrId);
            }
            customerId = customer.getCustomerId();
        }
        List<CrmQuote> quotes = listQuotes(customerId, parsedOpportunityId);
        return crmListResult("customer_quotes", customer, "quotes", quotes,
            quotes.isEmpty() ? "未查询到报价" : "查询成功");
    }

    public Map<String, Object> listContractsByCustomerOrOpportunity(String customerCodeOrNameOrId, String opportunityId) {
        Long parsedOpportunityId = tryParseId(StringUtils.isBlank(opportunityId) ? "" : opportunityId.trim());
        CrmCustomer customer = null;
        Long customerId = null;
        if (parsedOpportunityId == null) {
            customer = resolveSingleCustomer(customerCodeOrNameOrId);
            if (customer == null) {
                return customerNotUnique("customer_contracts", "未找到唯一客户，无法查询合同", customerCodeOrNameOrId);
            }
            customerId = customer.getCustomerId();
        }
        List<CrmContract> contracts = listContracts(customerId, parsedOpportunityId);
        return crmListResult("customer_contracts", customer, "contracts", contracts,
            contracts.isEmpty() ? "客户存在，但未查询到合同" : "查询成功");
    }

    public Map<String, Object> listPaymentPlansByCustomerOrContract(String customerCodeOrNameOrId, String contractId) {
        Long parsedContractId = tryParseId(StringUtils.isBlank(contractId) ? "" : contractId.trim());
        CrmCustomer customer = null;
        Long customerId = null;
        if (parsedContractId == null) {
            customer = resolveSingleCustomer(customerCodeOrNameOrId);
            if (customer == null) {
                return customerNotUnique("customer_payment_plans", "未找到唯一客户，无法查询回款计划", customerCodeOrNameOrId);
            }
            customerId = customer.getCustomerId();
        }
        List<CrmPaymentPlan> paymentPlans = listPaymentPlans(customerId, parsedContractId);
        return crmListResult("customer_payment_plans", customer, "paymentPlans", paymentPlans,
            paymentPlans.isEmpty() ? "未查询到回款计划" : "查询成功");
    }

    public Map<String, Object> getBusinessDocumentDetail(String documentType, String documentId) {
        Long id = tryParseId(StringUtils.isBlank(documentId) ? "" : documentId.trim());
        if (id == null) {
            return result(false, "crm_document_detail", "业务单据ID格式不正确", Map.of(
                "documentType", Objects.toString(documentType, ""),
                "documentId", Objects.toString(documentId, "")
            ));
        }

        String normalizedType = normalizeDocumentType(documentType);
        Object document = switch (normalizedType) {
            case "customer" -> customerMapper.selectById(id);
            case "contact" -> contactMapper.selectById(id);
            case "opportunity" -> opportunityMapper.selectById(id);
            case "quote" -> quoteMapper.selectById(id);
            case "contract" -> contractMapper.selectById(id);
            case "paymentPlan" -> paymentPlanMapper.selectById(id);
            case "followRecord" -> followRecordMapper.selectById(id);
            default -> null;
        };
        if (document == null && !"unknown".equals(normalizedType)) {
            return result(false, "crm_document_detail", "未查询到业务单据详情", Map.of(
                "documentType", normalizedType,
                "documentId", id
            ));
        }
        if ("unknown".equals(normalizedType)) {
            return result(false, "crm_document_detail", "不支持的业务单据类型", Map.of(
                "supportedTypes", List.of("customer", "contact", "opportunity", "quote", "contract", "paymentPlan", "followRecord"),
                "documentType", Objects.toString(documentType, "")
            ));
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("documentType", normalizedType);
        data.put("documentId", id);
        data.put("document", document);
        return result(true, "crm_document_detail", "查询成功", data);
    }

    public Map<String, Object> draftFollowRecord(String customerCodeOrNameOrId, String content, String followMethod,
                                                  String resultText, String followDateText, String nextFollowDateText) {
        CrmCustomer customer = resolveSingleCustomer(customerCodeOrNameOrId);
        if (customer == null) {
            return result(false, "crm_follow_record_draft", "未找到唯一客户，无法生成入库草稿", Map.of(
                "input", customerCodeOrNameOrId,
                "candidates", searchCustomers(customerCodeOrNameOrId),
                "needUserSelection", true
            ));
        }

        Map<String, Object> draft = buildFollowRecordDraft(customer, content, followMethod, resultText, followDateText, nextFollowDateText);
        FollowRecordDraft savedDraft = new FollowRecordDraft(
            customer.getCustomerId(),
            String.valueOf(draft.get("followDate")),
            String.valueOf(draft.get("followMethod")),
            String.valueOf(draft.get("content")),
            String.valueOf(draft.get("result")),
            draft.get("nextFollowDate") == null ? "" : String.valueOf(draft.get("nextFollowDate"))
        );
        String draftId = UUID.randomUUID().toString();
        RedisUtils.setCacheObject(followDraftKey(draftId), JsonUtils.toJsonString(savedDraft), FOLLOW_DRAFT_TTL);
        draft.put("needConfirm", true);
        draft.put("action", "confirm_required");
        draft.put("confirmInstruction", "用户确认后，调用 crmCreateFollowRecordConfirmed，并传入 draftId");
        draft.put("draftId", draftId);
        draft.put("expiresInSeconds", FOLLOW_DRAFT_TTL.toSeconds());
        return result(true, "crm_follow_record_draft", "已生成 CRM 跟进记录草稿，等待用户确认", draft);
    }

    public Map<String, Object> createFollowRecordConfirmed(String draftId) {
        String draftKey = followDraftKey(draftId);
        String draftJson = RedisUtils.getCacheObject(draftKey);
        FollowRecordDraft draft = StringUtils.isBlank(draftJson) ? null : JsonUtils.parseObject(draftJson, FollowRecordDraft.class);
        if (draft == null) {
            return result(false, "crm_follow_record_create", "草稿不存在或已入库，请重新生成跟进记录草稿", Map.of("draftId", draftId));
        }
        RedisUtils.deleteObject(draftKey);
        CrmCustomer customer = customerMapper.selectById(draft.customerId());
        if (customer == null) {
            return result(false, "crm_follow_record_create", "客户不存在，无法新增跟进记录", Map.of("customerId", draft.customerId()));
        }

        CrmFollowRecord record = new CrmFollowRecord();
        record.setCustomerId(draft.customerId());
        record.setFollowTime(toDateOrDefault(draft.followDate(), LocalDate.now(ZoneId.systemDefault())));
        record.setFollowMethod(draft.followMethod());
        record.setContent(draft.content());
        record.setResult(draft.result());
        if (StringUtils.isNotBlank(draft.nextFollowDate())) {
            record.setNextFollowTime(toDateOrNull(draft.nextFollowDate()));
        }

        boolean inserted = followRecordMapper.insert(record) > 0;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customer", customer);
        data.put("followRecord", record);
        return result(inserted, "crm_follow_record_create", inserted ? "CRM 跟进记录已入库" : "CRM 跟进记录入库失败", data);
    }

    private List<CrmContact> listContacts(Long customerId) {
        return contactMapper.selectList(new LambdaQueryWrapper<CrmContact>()
            .eq(CrmContact::getCustomerId, customerId)
            .orderByDesc(CrmContact::getCreateTime)
            .last(LIMIT_10));
    }

    private List<CrmOpportunity> listOpportunities(Long customerId) {
        return opportunityMapper.selectList(new LambdaQueryWrapper<CrmOpportunity>()
            .eq(CrmOpportunity::getCustomerId, customerId)
            .orderByDesc(CrmOpportunity::getCreateTime)
            .last(LIMIT_10));
    }

    private List<CrmQuote> listQuotes(Long customerId, Long opportunityId) {
        return quoteMapper.selectList(new LambdaQueryWrapper<CrmQuote>()
            .eq(opportunityId != null, CrmQuote::getOpportunityId, opportunityId)
            .eq(opportunityId == null && customerId != null, CrmQuote::getCustomerId, customerId)
            .orderByDesc(CrmQuote::getCreateTime)
            .last(LIMIT_10));
    }

    private List<CrmContract> listContracts(Long customerId, Long opportunityId) {
        return contractMapper.selectList(new LambdaQueryWrapper<CrmContract>()
            .eq(opportunityId != null, CrmContract::getOpportunityId, opportunityId)
            .eq(opportunityId == null && customerId != null, CrmContract::getCustomerId, customerId)
            .orderByDesc(CrmContract::getSignedDate)
            .orderByDesc(CrmContract::getCreateTime)
            .last(LIMIT_10));
    }

    private List<CrmPaymentPlan> listPaymentPlans(Long customerId, Long contractId) {
        return paymentPlanMapper.selectList(new LambdaQueryWrapper<CrmPaymentPlan>()
            .eq(contractId != null, CrmPaymentPlan::getContractId, contractId)
            .eq(contractId == null && customerId != null, CrmPaymentPlan::getCustomerId, customerId)
            .orderByAsc(CrmPaymentPlan::getPlannedDate)
            .orderByDesc(CrmPaymentPlan::getCreateTime)
            .last(LIMIT_10));
    }

    private List<CrmFollowRecord> listFollowRecords(Long customerId) {
        return followRecordMapper.selectList(new LambdaQueryWrapper<CrmFollowRecord>()
            .eq(CrmFollowRecord::getCustomerId, customerId)
            .orderByDesc(CrmFollowRecord::getFollowTime)
            .orderByDesc(CrmFollowRecord::getCreateTime)
            .last(LIMIT_10));
    }

    private Map<String, Object> customerNotUnique(String type, String message, String input) {
        return result(false, type, message, Map.of(
            "input", Objects.toString(input, ""),
            "candidates", searchCustomers(input),
            "needUserSelection", true
        ));
    }

    private Map<String, Object> customerListResult(String type, CrmCustomer customer, String listKey, List<?> list, String message) {
        return crmListResult(type, customer, listKey, list, message);
    }

    private Map<String, Object> crmListResult(String type, CrmCustomer customer, String listKey, List<?> list, String message) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (customer != null) {
            data.put("customer", customer);
        }
        data.put(listKey, list);
        data.put("count", list.size());
        return result(true, type, message, data);
    }

    private Map<String, Object> timelineItem(String type, Date time, Object data) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", type);
        item.put("time", time);
        item.put("data", data);
        return item;
    }

    private String normalizeDocumentType(String documentType) {
        if (StringUtils.isBlank(documentType)) {
            return "unknown";
        }
        String value = documentType.trim().toLowerCase();
        return switch (value) {
            case "customer", "客户", "crm_customer" -> "customer";
            case "contact", "联系人", "crm_contact" -> "contact";
            case "opportunity", "商机", "crm_opportunity" -> "opportunity";
            case "quote", "报价", "crm_quote" -> "quote";
            case "contract", "合同", "crm_contract" -> "contract";
            case "payment", "paymentplan", "payment_plan", "回款", "回款计划", "crm_payment_plan" -> "paymentPlan";
            case "follow", "followrecord", "follow_record", "跟进", "跟进记录", "crm_follow_record" -> "followRecord";
            default -> "unknown";
        };
    }

    private Map<String, Object> buildFollowRecordDraft(CrmCustomer customer, String content, String followMethod,
                                                        String resultText, String followDateText, String nextFollowDateText) {
        String normalizedMethod = normalizeFollowMethod(followMethod, content);
        String followDate = normalizeDate(followDateText, LocalDate.now(ZoneId.systemDefault()));
        String nextFollowDate = StringUtils.isBlank(nextFollowDateText) ? null : normalizeDate(nextFollowDateText, null);
        String normalizedResult = normalizeFollowResult(resultText, content);

        Map<String, Object> draft = new LinkedHashMap<>();
        draft.put("customerId", String.valueOf(customer.getCustomerId()));
        draft.put("customerCode", customer.getCode());
        draft.put("customerName", customer.getName());
        draft.put("followDate", followDate);
        draft.put("followMethod", normalizedMethod);
        draft.put("content", content);
        draft.put("result", normalizedResult);
        draft.put("nextFollowDate", nextFollowDate);
        return draft;
    }

    private CrmCustomer resolveSingleCustomer(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        String keyword = value.trim();
        Long id = tryParseId(keyword);
        if (id != null) {
            return customerMapper.selectById(id);
        }

        List<CrmCustomer> exact = customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
            .eq(CrmCustomer::getCode, keyword)
            .or()
            .eq(CrmCustomer::getName, keyword)
            .or()
            .eq(CrmCustomer::getShortName, keyword)
            .last("limit 2"));
        if (exact.size() == 1) {
            return exact.get(0);
        }

        List<CrmCustomer> fuzzy = searchCustomers(keyword);
        return fuzzy.size() == 1 ? fuzzy.get(0) : null;
    }

    private List<CrmCustomer> searchCustomers(String keyword) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        return customerMapper.selectList(new LambdaQueryWrapper<CrmCustomer>()
            .and(wrapper -> wrapper
                .like(CrmCustomer::getName, keyword)
                .or()
                .like(CrmCustomer::getShortName, keyword)
                .or()
                .like(CrmCustomer::getCode, keyword))
            .orderByDesc(CrmCustomer::getCreateTime)
            .last(LIMIT_10));
    }

    private String normalizeFollowMethod(String followMethod, String content) {
        String text = (StringUtils.isNotBlank(followMethod) ? followMethod : content) == null ? "" : (StringUtils.isNotBlank(followMethod) ? followMethod : content).trim();
        if (StringUtils.isBlank(text) || isUnsetText(text)) {
            return "onsite";
        }
        String lower = text.toLowerCase();
        if (List.of("phone", "wechat", "email", "onsite", "video").contains(lower)) {
            return lower;
        }
        if (text.contains("拜访") || text.contains("到访") || text.contains("走访")) {
            return "onsite";
        }
        if (text.contains("电话")) {
            return "phone";
        }
        if (text.contains("微信")) {
            return "wechat";
        }
        if (text.contains("邮件")) {
            return "email";
        }
        if (text.contains("视频") || text.contains("会议")) {
            return "video";
        }
        if (text.contains("现场")) {
            return "onsite";
        }
        return "onsite";
    }

    private String normalizeFollowResult(String resultText, String content) {
        String text = StringUtils.isNotBlank(resultText) ? resultText.trim() : "";
        String lower = text.toLowerCase();
        if (List.of("continue", "waiting", "paused", "failed").contains(lower)) {
            return lower;
        }
        String source = StringUtils.isNotBlank(text) && !isUnsetText(text) ? text : (content == null ? "" : content);
        if (source.contains("失败") || source.contains("拒绝") || source.contains("无意向")) {
            return "failed";
        }
        if (source.contains("暂停") || source.contains("搁置") || source.contains("暂缓")) {
            return "paused";
        }
        if (source.contains("等待") || source.contains("待反馈") || source.contains("下次") || source.contains("再沟通")) {
            return "waiting";
        }
        return "continue";
    }

    private String normalizeDate(String dateText, LocalDate defaultDate) {
        if (StringUtils.isBlank(dateText) || isUnsetText(dateText)) {
            return defaultDate != null ? defaultDate.toString() : null;
        }
        String text = dateText.trim();
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        return switch (text) {
            case "今天", "今日" -> today.toString();
            case "昨天" -> today.minusDays(1).toString();
            case "明天" -> today.plusDays(1).toString();
            default -> normalizeDateLiteral(text, defaultDate);
        };
    }

    private String normalizeDateLiteral(String text, LocalDate defaultDate) {
        if (text.length() >= 10 && text.charAt(4) == '-' && text.charAt(7) == '-') {
            return text.substring(0, 10);
        }
        return defaultDate != null ? defaultDate.toString() : text;
    }

    private Date toDateOrDefault(String date, LocalDate defaultDate) {
        LocalDate parsed = parseDateOrNull(date);
        LocalDate value = parsed != null ? parsed : defaultDate;
        return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date toDateOrNull(String date) {
        LocalDate parsed = parseDateOrNull(date);
        return parsed == null ? null : Date.from(parsed.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private LocalDate parseDateOrNull(String date) {
        if (StringUtils.isBlank(date) || isUnsetText(date)) {
            return null;
        }
        String normalized = normalizeDateLiteral(date.trim(), null);
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException e) {
            log.warn("CRM跟进日期无法解析，将使用默认处理: {}", date);
            return null;
        }
    }

    private boolean isUnsetText(String text) {
        if (StringUtils.isBlank(text)) {
            return true;
        }
        String value = text.trim();
        return List.of("未定", "待定", "无", "暂无", "没有", "null", "NULL", "未知", "不确定").contains(value);
    }

    private String followDraftKey(String draftId) {
        if (StringUtils.isBlank(draftId)) {
            throw new IllegalArgumentException("draftId不能为空");
        }
        return FOLLOW_DRAFT_KEY_PREFIX + draftId;
    }

    private Long tryParseId(String value) {
        try {
            return Long.parseLong(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Map<String, Object> result(boolean success, String type, String message, Object data) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("type", type);
        result.put("message", message);
        result.put("data", data);
        return result;
    }

    private record FollowRecordDraft(Long customerId, String followDate, String followMethod, String content, String result, String nextFollowDate) {
    }
}
