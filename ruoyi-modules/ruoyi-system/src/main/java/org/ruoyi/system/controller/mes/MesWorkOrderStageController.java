package org.ruoyi.system.controller.mes;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.mes.MesWorkOrderStage;
import org.ruoyi.system.service.mes.MesWorkOrderStageService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/mes/work-order-stage")
public class MesWorkOrderStageController extends XtpCrudController<MesWorkOrderStage> {

    private final MesWorkOrderStageService service;

    @Override
    protected XtpCrudService<MesWorkOrderStage> service() {
        return service;
    }

    @Override
    protected String title() {
        return "工单阶段";
    }

    @Override
    protected Class<MesWorkOrderStage> entityClass() {
        return MesWorkOrderStage.class;
    }
}
