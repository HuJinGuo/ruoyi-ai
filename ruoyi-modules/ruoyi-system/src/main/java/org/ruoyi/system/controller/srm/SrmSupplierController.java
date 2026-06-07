package org.ruoyi.system.controller.srm;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.srm.SrmSupplier;
import org.ruoyi.system.service.srm.SrmSupplierService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/srm/supplier")
public class SrmSupplierController extends XtpCrudController<SrmSupplier> {

    private final SrmSupplierService service;

    @Override
    protected XtpCrudService<SrmSupplier> service() {
        return service;
    }

    @Override
    protected String title() {
        return "供应商";
    }

    @Override
    protected Class<SrmSupplier> entityClass() {
        return SrmSupplier.class;
    }
}
