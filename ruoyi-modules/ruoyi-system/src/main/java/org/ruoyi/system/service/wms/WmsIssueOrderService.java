package org.ruoyi.system.service.wms;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.crm.CrmContract;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.wms.WmsIssueOrder;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.wms.WmsIssueOrderMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WmsIssueOrderService extends XtpCrudService<WmsIssueOrder> {

    private final MesWorkOrderMapper workOrderMapper;
    private final CrmContractMapper contractMapper;
    private final SysUserMapper userMapper;

    public WmsIssueOrderService(WmsIssueOrderMapper mapper, MesWorkOrderMapper workOrderMapper,
                                CrmContractMapper contractMapper, SysUserMapper userMapper) {
        super(mapper);
        this.workOrderMapper = workOrderMapper;
        this.contractMapper = contractMapper;
        this.userMapper = userMapper;
    }

    @Override
    protected Wrapper<WmsIssueOrder> buildQueryWrapper(WmsIssueOrder query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<WmsIssueOrder>()
            .eq(query.getWorkOrderId() != null, WmsIssueOrder::getWorkOrderId, query.getWorkOrderId())
            .eq(query.getContractId() != null, WmsIssueOrder::getContractId, query.getContractId())
            .eq(StringUtils.isNotBlank(query.getIssueStatus()), WmsIssueOrder::getIssueStatus, query.getIssueStatus())
            .eq(query.getWarehouseUserId() != null, WmsIssueOrder::getWarehouseUserId, query.getWarehouseUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), WmsIssueOrder::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, WmsIssueOrder::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(WmsIssueOrder::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<WmsIssueOrder> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> workOrderIds = records.stream().map(WmsIssueOrder::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> contractIds = records.stream().map(WmsIssueOrder::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(WmsIssueOrder::getWarehouseUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, CrmContract> contracts = contractIds.isEmpty() ? Map.of() : contractMapper.selectList(
            new LambdaQueryWrapper<CrmContract>().in(CrmContract::getContractId, contractIds)
        ).stream().collect(Collectors.toMap(CrmContract::getContractId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds)
        ).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            MesWorkOrder workOrder = workOrders.get(record.getWorkOrderId());
            if (workOrder != null) {
                record.setWorkOrderCode(workOrder.getWorkOrderCode());
                record.setProjectName(workOrder.getProjectName());
            }
            CrmContract contract = contracts.get(record.getContractId());
            if (contract != null) {
                record.setContractName(contract.getName());
            }
            record.setWarehouseUserName(userName(users.get(record.getWarehouseUserId())));
        });
    }

    private String userName(SysUser user) {
        if (user == null) {
            return null;
        }
        return StringUtils.isNotBlank(user.getNickName()) ? user.getNickName() : user.getUserName();
    }
}
