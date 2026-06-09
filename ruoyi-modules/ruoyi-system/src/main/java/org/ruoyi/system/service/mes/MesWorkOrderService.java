package org.ruoyi.system.service.mes;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.crm.CrmCustomer;
import org.ruoyi.system.domain.crm.CrmOpportunity;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.crm.CrmCustomerMapper;
import org.ruoyi.system.mapper.crm.CrmOpportunityMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MesWorkOrderService extends XtpCrudService<MesWorkOrder> {

    private final CrmContractMapper contractMapper;
    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final SysUserMapper userMapper;

    public MesWorkOrderService(MesWorkOrderMapper mapper, CrmContractMapper contractMapper,
                               CrmCustomerMapper customerMapper, CrmOpportunityMapper opportunityMapper,
                               SysUserMapper userMapper) {
        super(mapper);
        this.contractMapper = contractMapper;
        this.customerMapper = customerMapper;
        this.opportunityMapper = opportunityMapper;
        this.userMapper = userMapper;
    }

    @Override
    protected Wrapper<MesWorkOrder> buildQueryWrapper(MesWorkOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<MesWorkOrder>()
            .eq(query.getContractId() != null, MesWorkOrder::getContractId, query.getContractId())
            .eq(query.getCustomerId() != null, MesWorkOrder::getCustomerId, query.getCustomerId())
            .eq(query.getOpportunityId() != null, MesWorkOrder::getOpportunityId, query.getOpportunityId())
            .like(StringUtils.isNotBlank(query.getWorkOrderCode()), MesWorkOrder::getWorkOrderCode, query.getWorkOrderCode())
            .like(StringUtils.isNotBlank(query.getProjectName()), MesWorkOrder::getProjectName, query.getProjectName())
            .like(StringUtils.isNotBlank(query.getProductName()), MesWorkOrder::getProductName, query.getProductName())
            .eq(StringUtils.isNotBlank(query.getCurrentStage()), MesWorkOrder::getCurrentStage, query.getCurrentStage())
            .eq(StringUtils.isNotBlank(query.getStatus()), MesWorkOrder::getStatus, query.getStatus())
            .eq(query.getResponsibleUserId() != null, MesWorkOrder::getResponsibleUserId, query.getResponsibleUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), MesWorkOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, MesWorkOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(MesWorkOrder::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<MesWorkOrder> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> contractIds = records.stream().map(MesWorkOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> customerIds = records.stream().map(MesWorkOrder::getCustomerId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> opportunityIds = records.stream().map(MesWorkOrder::getOpportunityId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(MesWorkOrder::getResponsibleUserId).filter(Objects::nonNull).collect(Collectors.toCollection(HashSet::new));

        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, CrmCustomer> customers = customerIds.isEmpty() ? Map.of() : customerMapper.selectList(
            new LambdaQueryWrapper<CrmCustomer>().in(CrmCustomer::getCustomerId, customerIds)
        ).stream().collect(Collectors.toMap(CrmCustomer::getCustomerId, Function.identity(), (a, b) -> a));
        Map<Long, CrmOpportunity> opportunities = opportunityIds.isEmpty() ? Map.of() : opportunityMapper.selectList(
            new LambdaQueryWrapper<CrmOpportunity>().in(CrmOpportunity::getOpportunityId, opportunityIds)
        ).stream().collect(Collectors.toMap(CrmOpportunity::getOpportunityId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds)
        ).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));

        records.forEach(record -> {
            CrmContract contract = contracts.get(record.getContractId());
            if (contract != null) {
                record.setContractName(contract.getName());
            }
            CrmCustomer customer = customers.get(record.getCustomerId());
            if (customer != null) {
                record.setCustomerCode(customer.getCode());
                record.setCustomerName(customer.getName());
            }
            CrmOpportunity opportunity = opportunities.get(record.getOpportunityId());
            if (opportunity != null) {
                record.setOpportunityName(opportunity.getName());
            }
            record.setResponsibleUserName(userName(users.get(record.getResponsibleUserId())));
        });
    }

    private String userName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName();
    }
}
