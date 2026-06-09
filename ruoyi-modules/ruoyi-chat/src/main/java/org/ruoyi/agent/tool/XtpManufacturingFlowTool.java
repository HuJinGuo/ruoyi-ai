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

    @Tool("按合同名称查询 CRM 合同，用于用户只提供合同名时定位 XTP 制造闭环合同")
    public String crmContractQueryByName(String contractName) {
        log.info("【XTP工具调用】crmContractQueryByName contractName={}", contractName);
        return toJson(SpringUtils.getBean(CrmContractMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.crm.CrmContract>()
                .like(org.ruoyi.system.domain.crm.CrmContract::getName, contractName)
                .orderByDesc(org.ruoyi.system.domain.crm.CrmContract::getCreateTime)));
    }

    @Tool("按合同名称直接汇总 XTP 制造闭环状态，避免模型转抄长合同ID出错")
    public String xtpManufacturingStatusByContractName(String contractName) {
        log.info("【XTP工具调用】xtpManufacturingStatusByContractName contractName={}", contractName);
        org.ruoyi.system.domain.crm.CrmContract contract = SpringUtils.getBean(CrmContractMapper.class)
            .selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.crm.CrmContract>()
                .like(org.ruoyi.system.domain.crm.CrmContract::getName, contractName)
                .orderByDesc(org.ruoyi.system.domain.crm.CrmContract::getCreateTime)
                .last("limit 1"));
        if (contract == null) {
            return toJson(java.util.Map.of("contractName", contractName, "message", "合同不存在"));
        }
        return xtpManufacturingStatusByContractId(contract.getContractId());
    }

    @Tool("按合同ID汇总 XTP 制造闭环状态，包含工单、阶段、工程物料、采购、收料、发料和库存")
    public String xtpManufacturingStatusByContractId(Long contractId) {
        log.info("【XTP工具调用】xtpManufacturingStatusByContractId contractId={}", contractId);
        return toJson(buildManufacturingStatusByContractId(contractId));
    }

    private java.util.Map<String, Object> buildManufacturingStatusByContractId(Long contractId) {
        CrmContractMapper contractMapper = SpringUtils.getBean(CrmContractMapper.class);
        MesWorkOrderMapper workOrderMapper = SpringUtils.getBean(MesWorkOrderMapper.class);
        org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper stageMapper = SpringUtils.getBean(org.ruoyi.system.mapper.mes.MesWorkOrderStageMapper.class);
        EngineeringMaterialMapper materialMapper = SpringUtils.getBean(EngineeringMaterialMapper.class);
        SrmPurchaseRequestMapper purchaseRequestMapper = SpringUtils.getBean(SrmPurchaseRequestMapper.class);
        org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper purchaseOrderMapper = SpringUtils.getBean(org.ruoyi.system.mapper.srm.SrmPurchaseOrderMapper.class);
        org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper receiptOrderMapper = SpringUtils.getBean(org.ruoyi.system.mapper.wms.WmsReceiptOrderMapper.class);
        org.ruoyi.system.mapper.wms.WmsIssueOrderMapper issueOrderMapper = SpringUtils.getBean(org.ruoyi.system.mapper.wms.WmsIssueOrderMapper.class);
        WmsInventoryMapper inventoryMapper = SpringUtils.getBean(WmsInventoryMapper.class);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        org.ruoyi.system.domain.crm.CrmContract contract = contractMapper.selectById(contractId);
        result.put("contract", contract);
        java.util.List<org.ruoyi.system.domain.mes.MesWorkOrder> workOrders = workOrderMapper
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.mes.MesWorkOrder>()
                .eq(org.ruoyi.system.domain.mes.MesWorkOrder::getContractId, contractId)
                .orderByDesc(org.ruoyi.system.domain.mes.MesWorkOrder::getCreateTime));
        java.util.List<java.util.Map<String, Object>> workOrderDetails = new java.util.ArrayList<>();
        for (org.ruoyi.system.domain.mes.MesWorkOrder workOrder : workOrders) {
            java.util.Map<String, Object> detail = new java.util.LinkedHashMap<>();
            Long workOrderId = workOrder.getWorkOrderId();
            detail.put("workOrder", workOrder);
            detail.put("stages", stageMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.mes.MesWorkOrderStage>()
                .eq(org.ruoyi.system.domain.mes.MesWorkOrderStage::getWorkOrderId, workOrderId)
                .orderByAsc(org.ruoyi.system.domain.mes.MesWorkOrderStage::getCreateTime)));
            java.util.List<org.ruoyi.system.domain.engineering.EngineeringMaterial> materials = materialMapper
                .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.engineering.EngineeringMaterial>()
                    .eq(org.ruoyi.system.domain.engineering.EngineeringMaterial::getWorkOrderId, workOrderId));
            detail.put("engineeringMaterials", materials);
            detail.put("purchaseRequests", purchaseRequestMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.srm.SrmPurchaseRequest>()
                .eq(org.ruoyi.system.domain.srm.SrmPurchaseRequest::getWorkOrderId, workOrderId)));
            detail.put("purchaseOrders", purchaseOrderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.srm.SrmPurchaseOrder>()
                .eq(org.ruoyi.system.domain.srm.SrmPurchaseOrder::getWorkOrderId, workOrderId)));
            detail.put("receiptOrders", receiptOrderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.wms.WmsReceiptOrder>()
                .eq(org.ruoyi.system.domain.wms.WmsReceiptOrder::getWorkOrderId, workOrderId)));
            detail.put("issueOrders", issueOrderMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.wms.WmsIssueOrder>()
                .eq(org.ruoyi.system.domain.wms.WmsIssueOrder::getWorkOrderId, workOrderId)));

            java.util.Map<Long, Object> inventoryByPart = new java.util.LinkedHashMap<>();
            for (org.ruoyi.system.domain.engineering.EngineeringMaterial material : materials) {
                if (material.getPartId() != null) {
                    inventoryByPart.put(material.getPartId(), inventoryMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.wms.WmsInventory>()
                        .eq(org.ruoyi.system.domain.wms.WmsInventory::getPartId, material.getPartId())));
                }
            }
            detail.put("inventoryByPart", inventoryByPart);
            workOrderDetails.add(detail);
        }
        result.put("workOrderDetails", workOrderDetails);
        return result;
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

    @Tool("按合同ID查询 MES 生产工单，用于查看合同对应的制造闭环进度")
    public String mesWorkOrderQueryByContractId(Long contractId) {
        log.info("【XTP工具调用】mesWorkOrderQueryByContractId contractId={}", contractId);
        return toJson(SpringUtils.getBean(MesWorkOrderMapper.class)
            .selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<org.ruoyi.system.domain.mes.MesWorkOrder>()
                .eq(org.ruoyi.system.domain.mes.MesWorkOrder::getContractId, contractId)
                .orderByDesc(org.ruoyi.system.domain.mes.MesWorkOrder::getCreateTime)));
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
        if (value == null) {
            return "{\"data\":null,\"message\":\"未查询到数据\"}";
        }
        try {
            String json = JsonUtils.toJsonString(value);
            return json == null || json.isBlank()
                ? "{\"data\":null,\"message\":\"未查询到数据\"}"
                : json;
        } catch (Exception e) {
            log.error("XTP制造闭环工具返回值序列化失败", e);
            return "Error: " + e.getMessage();
        }
    }

}
