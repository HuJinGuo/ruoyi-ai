package org.ruoyi.system.controller.srm;

import lombok.RequiredArgsConstructor;
import org.ruoyi.system.controller.xtp.XtpCrudController;
import org.ruoyi.system.domain.srm.SrmPurchaseRequest;
import org.ruoyi.system.service.srm.SrmPurchaseRequestService;
import org.ruoyi.system.service.xtp.XtpCrudService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/srm/purchase-request")
public class SrmPurchaseRequestController extends XtpCrudController<SrmPurchaseRequest> {

    private final SrmPurchaseRequestService service;

    @Override
    protected XtpCrudService<SrmPurchaseRequest> service() {
        return service;
    }

    @Override
    protected String title() {
        return "采购需求";
    }

    @Override
    protected Class<SrmPurchaseRequest> entityClass() {
        return SrmPurchaseRequest.class;
    }
}
