package org.ruoyi.system.service.crm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.system.domain.crm.CrmContact;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.crm.CrmQuote;
import org.ruoyi.system.mapper.crm.CrmContactMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.crm.CrmQuoteMapper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CRM 列表展示字段批量查询工具。
 */
final class CrmDisplayFields {

    private CrmDisplayFields() {
    }

    static <V> Set<Long> ids(List<V> records, Function<V, Long> idGetter) {
        return records.stream()
            .map(idGetter)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    static Map<Long, CrmCustomer> customerMap(CrmCustomerMapper mapper, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getCustomerId, ids))
            .stream()
            .collect(Collectors.toMap(CrmCustomer::getCustomerId, Function.identity(), (a, b) -> a));
    }

    static Map<Long, CrmContact> contactMap(CrmContactMapper mapper, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CrmContact>().in(CrmContact::getContactId, ids))
            .stream()
            .collect(Collectors.toMap(CrmContact::getContactId, Function.identity(), (a, b) -> a));
    }

    static Map<Long, CrmOpportunity> opportunityMap(CrmOpportunityMapper mapper, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CrmOpportunity>().in(CrmOpportunity::getOpportunityId, ids))
            .stream()
            .collect(Collectors.toMap(CrmOpportunity::getOpportunityId, Function.identity(), (a, b) -> a));
    }

    static Map<Long, CrmQuote> quoteMap(CrmQuoteMapper mapper, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CrmQuote>().in(CrmQuote::getQuoteId, ids))
            .stream()
            .collect(Collectors.toMap(CrmQuote::getQuoteId, Function.identity(), (a, b) -> a));
    }

    static Map<Long, CrmContract> contractMap(CrmContractMapper mapper, Collection<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return mapper.selectList(new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, ids))
            .stream()
            .collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
    }

    static String quoteName(CrmQuote quote) {
        if (quote == null) {
            return null;
        }
        return "报价 #" + quote.getQuoteId() + " / V" + Objects.toString(quote.getVersion(), "-");
    }
}
