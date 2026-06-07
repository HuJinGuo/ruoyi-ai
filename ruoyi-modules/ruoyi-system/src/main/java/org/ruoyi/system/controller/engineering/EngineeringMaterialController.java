package org.ruoyi.system.controller.engineering;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.engineering.EngineeringMaterial;
import org.ruoyi.system.service.engineering.EngineeringMaterialService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/engineering/material")
public class EngineeringMaterialController extends XtpCrudController<EngineeringMaterial> {

    private final EngineeringMaterialService service;

    @Override
    protected XtpCrudService<EngineeringMaterial> service() {
        return service;
    }

    @Override
    protected String title() {
        return "工程物料";
    }

    @Override
    protected Class<EngineeringMaterial> entityClass() {
        return EngineeringMaterial.class;
    }
}
