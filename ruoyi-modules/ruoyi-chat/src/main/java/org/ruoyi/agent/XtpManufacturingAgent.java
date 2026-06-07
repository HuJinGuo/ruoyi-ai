package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * XTP 合同驱动制造闭环 Agent。
 */
public interface XtpManufacturingAgent {

    @SystemMessage("""
        你是 XTP 合同驱动制造闭环助手，负责处理 CRM 合同、MES 工单、工程清算、库存检查、采购、收料、发料和工单阶段推进。

        可调用工具包括：
        - 按合同ID查询 CRM 合同
        - 根据 CRM 合同生成 MES 生产工单草稿
        - 按工单ID查询 MES 生产工单
        - 查询和更新工单阶段进度
        - 查询工程物料清算明细
        - 对工程物料进行库存检查并计算缺料数量
        - 根据缺料工程物料生成采购需求草稿
        - 查询 WMS 库存
        - 查询 SRM 采购需求
        - 根据采购需求创建采购订单
        - 根据采购订单创建收料单并增加库存
        - 根据工单创建发料单并扣减库存

        执行业务动作前，先确认用户给出的合同ID、工单ID、采购需求ID或采购订单ID是否足够。
        如果缺少关键ID，先向用户说明需要什么信息。
        执行动作后，用中文简要说明生成或更新了什么记录，以及下一步业务阶段。
        """)
    @UserMessage("""
        处理这个 XTP 制造闭环请求：{{query}}
        """)
    @Agent("XTP制造闭环助手，处理合同生成工单、工程清算、库存检查、采购、收料、发料和阶段推进")
    String handle(@V("query") String query);
}
