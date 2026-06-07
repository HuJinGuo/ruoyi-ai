package org.ruoyi.system.controller.material;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.material.MaterialPart;
import org.ruoyi.system.service.material.MaterialPartService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/material/part")
public class MaterialPartController extends XtpCrudController<MaterialPart> {

    private final MaterialPartService service;

    @Override
    protected XtpCrudService<MaterialPart> service() {
        return service;
    }

    @Override
    protected String title() {
        return "物料";
    }

    @Override
    protected Class<MaterialPart> entityClass() {
        return MaterialPart.class;
    }
}
