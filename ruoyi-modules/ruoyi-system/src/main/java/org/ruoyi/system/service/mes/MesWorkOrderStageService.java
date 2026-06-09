package org.ruoyi.system.service.mes;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MesWorkOrderStageService extends XtpCrudService<MesWorkOrderStage> {

    private final MesWorkOrderMapper workOrderMapper;
    private final SysUserMapper userMapper;

    public MesWorkOrderStageService(MesWorkOrderStageMapper mapper, MesWorkOrderMapper workOrderMapper, SysUserMapper userMapper) {
        super(mapper);
        this.workOrderMapper = workOrderMapper;
        this.userMapper = userMapper;
    }

    @Override
    protected Wrapper<MesWorkOrderStage> buildQueryWrapper(MesWorkOrderStage query) {
        Map<String, Object> params = query.getParams();
        return new LambdaQueryWrapper<MesWorkOrderStage>()
            .eq(query.getWorkOrderId() != null, MesWorkOrderStage::getWorkOrderId, query.getWorkOrderId())
            .like(StringUtils.isNotBlank(query.getStageCode()), MesWorkOrderStage::getStageCode, query.getStageCode())
            .like(StringUtils.isNotBlank(query.getStageName()), MesWorkOrderStage::getStageName, query.getStageName())
            .eq(StringUtils.isNotBlank(query.getStatus()), MesWorkOrderStage::getStatus, query.getStatus())
            .eq(query.getResponsibleUserId() != null, MesWorkOrderStage::getResponsibleUserId, query.getResponsibleUserId())
            .eq(StringUtils.isNotBlank(query.getRemark()), MesWorkOrderStage::getRemark, query.getRemark())
            .between(params.get("beginTime") != null && params.get("endTime") != null, MesWorkOrderStage::getCreateTime, params.get("beginTime"), params.get("endTime"))
            .orderByDesc(MesWorkOrderStage::getCreateTime);
    }

    @Override
    protected void fillDisplayFields(List<MesWorkOrderStage> records) {
        if (records.isEmpty()) {
            return;
        }
        Set<Long> workOrderIds = records.stream().map(MesWorkOrderStage::getWorkOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> userIds = records.stream().map(MesWorkOrderStage::getResponsibleUserId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, MesWorkOrder> workOrders = workOrderIds.isEmpty() ? Map.of() : workOrderMapper.selectList(
            new LambdaQueryWrapper<MesWorkOrder>().in(MesWorkOrder::getWorkOrderId, workOrderIds)
        ).stream().collect(Collectors.toMap(MesWorkOrder::getWorkOrderId, Function.identity(), (a, b) -> a));
        Map<Long, SysUser> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(
            new LambdaQueryWrapper<SysUser>().in(SysUser::getUserId, userIds)
        ).stream().collect(Collectors.toMap(SysUser::getUserId, Function.identity(), (a, b) -> a));
        records.forEach(record -> {
            MesWorkOrder workOrder = workOrders.get(record.getWorkOrderId());
            if (workOrder != null) {
                record.setWorkOrderCode(workOrder.getWorkOrderCode());
                record.setProjectName(workOrder.getProjectName());
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
