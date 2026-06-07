package org.ruoyi.system.controller.mes;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.mes.MesWorkOrder;
import org.ruoyi.system.service.mes.MesWorkOrderService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mes/work-order")
public class MesWorkOrderController extends XtpCrudController<MesWorkOrder> {

    private final MesWorkOrderService service;

    @Override
    protected XtpCrudService<MesWorkOrder> service() {
        return service;
    }

    @Override
    protected String title() {
        return "生产工单";
    }

    @Override
    protected Class<MesWorkOrder> entityClass() {
        return MesWorkOrder.class;
    }
}
