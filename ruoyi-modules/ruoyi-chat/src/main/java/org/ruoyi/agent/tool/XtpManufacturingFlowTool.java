package org.ruoyi.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.ruoyi.system.mapper.crm.CrmContractMapper;
import org.ruoyi.system.mapper.engineering.EngineeringMaterialMapper;
import org.ruoyi.system.mapper.mes.MesWorkOrderMapper;
import org.ruoyi.system.mapper.srm.SrmPurchaseRequestMapper;
import org.ruoyi.system.mapper.wms.WmsInventoryMapper;
import org.ruoyi.system.service.xtp.XtpManufacturingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * XTP V1 合同驱动制造闭环 AI 工具。
 */
@Slf4j
@Component
public class XtpManufacturingFlowTool implements BuiltinToolProvider {

    private XtpManufacturingService flowService() {
        return SpringUtils.getBean(XtpManufacturingService.class);
    }

    @Tool("Query a CRM contract by contract id for XTP manufacturing flow")
    public String crmContractQuery(Long contractId) {
        return toJson(SpringUtils.getBean(CrmContractMapper.class).selectById(contractId));
    }

    @Tool("Generate an MES work order draft from an executing CRM contract")
    public String crmContractGenerateWorkOrder(Long contractId, String productName, Integer quantity, Long responsibleUserId) {
        return toJson(flowService().generateWorkOrder(contractId, productName, quantity, responsibleUserId));
    }

    @Tool("Query an MES work order by work order id")
    public String mesWorkOrderQuery(Long workOrderId) {
        return toJson(SpringUtils.getBean(MesWorkOrderMapper.class).selectById(workOrderId));
    }

    @Tool("Update a work order stage status. Status can be WAIT, PROCESSING, FINISHED, or PAUSE")
    public String mesWorkOrderStageUpdate(Long workOrderId, String stageCode, String status, String remark) {
        return toJson(flowService().updateStage(workOrderId, stageCode, status, remark));
    }

    @Tool("List stage progress for a work order")
    public String mesWorkOrderStages(Long workOrderId) {
        return toJson(flowService().listStages(workOrderId));
    }

    @Tool("List engineering material rows for a work order")
    public String engineeringMaterialQuery(Long workOrderId) {
        return toJson(SpringUtils.getBean(EngineeringMaterialMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.engineering.EngineeringMaterial>()
                .eq(org.ruoyi.system.domain.engineering.EngineeringMaterial::getWorkOrderId, workOrderId)));
    }

    @Tool("Check inventory for engineering material rows and calculate shortage quantity")
    public String engineeringMaterialCheckInventory(Long workOrderId) {
        return toJson(flowService().checkInventory(workOrderId));
    }

    @Tool("Generate purchase request drafts from shortage engineering material rows")
    public String engineeringMaterialGeneratePurchaseRequest(Long workOrderId) {
        return toJson(flowService().generatePurchaseRequests(workOrderId));
    }

    @Tool("Query WMS inventory rows by part id")
    public String wmsInventoryQuery(Long partId) {
        return toJson(SpringUtils.getBean(WmsInventoryMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.wms.WmsInventory>()
                .eq(org.ruoyi.system.domain.wms.WmsInventory::getPartId, partId)));
    }

    @Tool("List SRM purchase requests for a work order")
    public String srmPurchaseRequestQuery(Long workOrderId) {
        return toJson(SpringUtils.getBean(SrmPurchaseRequestMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.srm.SrmPurchaseRequest>()
                .eq(org.ruoyi.system.domain.srm.SrmPurchaseRequest::getWorkOrderId, workOrderId)));
    }

    @Tool("Create a purchase order from one purchase request")
    public String srmPurchaseOrderCreate(Long purchaseRequestId, BigDecimal price) {
        return toJson(flowService().createPurchaseOrder(purchaseRequestId, price, null));
    }

    @Tool("Create a receipt order from one purchase order and increase inventory")
    public String wmsReceiptCreate(Long purchaseOrderId, Long warehouseUserId) {
        return toJson(flowService().createReceiptOrder(purchaseOrderId, warehouseUserId));
    }

    @Tool("Create an issue order for a work order and decrease inventory")
    public String wmsIssueCreate(Long workOrderId, Long warehouseUserId) {
        return toJson(flowService().createIssueOrder(workOrderId, warehouseUserId));
    }

    @Override
    public String getToolName() {
        return "xtp_manufacturing_flow";
    }

    @Override
    public String getDisplayName() {
        return "XTP制造闭环工具";
    }

    @Override
    public String getDescription() {
        return "XTP V1 contract-driven manufacturing tools: contract query, work order generation, inventory check, purchase request/order, receipt, issue, and stage update.";
    }

    private String toJson(Object value) {
        try {
            return JsonUtils.toJsonString(value);
        } catch (Exception e) {
            log.error("XTP制造闭环工具返回值序列化失败", e);
            return "Error: " + e.getMessage();
        }
    }
}
