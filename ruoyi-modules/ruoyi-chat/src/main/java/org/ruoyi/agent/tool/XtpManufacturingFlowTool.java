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

    @Tool("按合同ID查询 CRM 合同，用于 XTP 制造闭环")
    public String crmContractQuery(Long contractId) {
        log.info("【XTP工具调用】crmContractQuery contractId={}", contractId);
        return toJson(SpringUtils.getBean(CrmContractMapper.class).selectById(contractId));
    }

    @Tool("根据 CRM 合同生成 MES 生产工单草稿")
    public String crmContractGenerateWorkOrder(Long contractId, String productName, Integer quantity, Long responsibleUserId) {
        log.info("【XTP工具调用】crmContractGenerateWorkOrder contractId={}, productName={}, quantity={}, responsibleUserId={}",
            contractId, productName, quantity, responsibleUserId);
        return toJson(flowService().generateWorkOrder(contractId, productName, quantity, responsibleUserId));
    }

    @Tool("按工单ID查询 MES 生产工单")
    public String mesWorkOrderQuery(Long workOrderId) {
        log.info("【XTP工具调用】mesWorkOrderQuery workOrderId={}", workOrderId);
        return toJson(SpringUtils.getBean(MesWorkOrderMapper.class).selectById(workOrderId));
    }

    @Tool("更新工单阶段状态，状态可为 WAIT、PROCESSING、FINISHED、PAUSE")
    public String mesWorkOrderStageUpdate(Long workOrderId, String stageCode, String status, String remark) {
        log.info("【XTP工具调用】mesWorkOrderStageUpdate workOrderId={}, stageCode={}, status={}",
            workOrderId, stageCode, status);
        return toJson(flowService().updateStage(workOrderId, stageCode, status, remark));
    }

    @Tool("查询工单阶段进度")
    public String mesWorkOrderStages(Long workOrderId) {
        log.info("【XTP工具调用】mesWorkOrderStages workOrderId={}", workOrderId);
        return toJson(flowService().listStages(workOrderId));
    }

    @Tool("查询工单的工程物料清算明细")
    public String engineeringMaterialQuery(Long workOrderId) {
        log.info("【XTP工具调用】engineeringMaterialQuery workOrderId={}", workOrderId);
        return toJson(SpringUtils.getBean(EngineeringMaterialMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.engineering.EngineeringMaterial>()
                .eq(org.ruoyi.system.domain.engineering.EngineeringMaterial::getWorkOrderId, workOrderId)));
    }

    @Tool("对工程物料进行库存检查并计算缺料数量")
    public String engineeringMaterialCheckInventory(Long workOrderId) {
        log.info("【XTP工具调用】engineeringMaterialCheckInventory workOrderId={}", workOrderId);
        return toJson(flowService().checkInventory(workOrderId));
    }

    @Tool("根据缺料工程物料生成采购需求草稿")
    public String engineeringMaterialGeneratePurchaseRequest(Long workOrderId) {
        log.info("【XTP工具调用】engineeringMaterialGeneratePurchaseRequest workOrderId={}", workOrderId);
        return toJson(flowService().generatePurchaseRequests(workOrderId));
    }

    @Tool("按物料ID查询 WMS 库存")
    public String wmsInventoryQuery(Long partId) {
        log.info("【XTP工具调用】wmsInventoryQuery partId={}", partId);
        return toJson(SpringUtils.getBean(WmsInventoryMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.wms.WmsInventory>()
                .eq(org.ruoyi.system.domain.wms.WmsInventory::getPartId, partId)));
    }

    @Tool("查询工单对应的 SRM 采购需求")
    public String srmPurchaseRequestQuery(Long workOrderId) {
        log.info("【XTP工具调用】srmPurchaseRequestQuery workOrderId={}", workOrderId);
        return toJson(SpringUtils.getBean(SrmPurchaseRequestMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.srm.SrmPurchaseRequest>()
                .eq(org.ruoyi.system.domain.srm.SrmPurchaseRequest::getWorkOrderId, workOrderId)));
    }

    @Tool("根据采购需求创建采购订单")
    public String srmPurchaseOrderCreate(Long purchaseRequestId, BigDecimal price) {
        log.info("【XTP工具调用】srmPurchaseOrderCreate purchaseRequestId={}, price={}", purchaseRequestId, price);
        return toJson(flowService().createPurchaseOrder(purchaseRequestId, price, null));
    }

    @Tool("根据采购订单创建收料单并增加库存")
    public String wmsReceiptCreate(Long purchaseOrderId, Long warehouseUserId) {
        log.info("【XTP工具调用】wmsReceiptCreate purchaseOrderId={}, warehouseUserId={}", purchaseOrderId, warehouseUserId);
        return toJson(flowService().createReceiptOrder(purchaseOrderId, warehouseUserId));
    }

    @Tool("根据工单创建发料单并扣减库存")
    public String wmsIssueCreate(Long workOrderId, Long warehouseUserId) {
        log.info("【XTP工具调用】wmsIssueCreate workOrderId={}, warehouseUserId={}", workOrderId, warehouseUserId);
        return toJson(flowService().createIssueOrder(workOrderId, warehouseUserId));
    }

    @Override
    public String getToolName() {
        return "XTP制造闭环工具";
    }

    @Override
    public String getDisplayName() {
        return "XTP制造闭环工具";
    }

    @Override
    public String getDescription() {
        return "XTP V1 合同驱动制造闭环工具：支持合同查询、生成工单、库存检查、采购需求/采购订单、收料、发料和阶段推进。";
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
