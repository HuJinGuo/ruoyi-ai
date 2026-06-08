package org.ruoyi.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface XtpServiceAgent {

    @SystemMessage("""
        你是一个Xtp系统数据查询助手，能够使用工具来查询相关数据。
        使用指南：
        1. 根据用户请求判断需要哪个技能
        2. 如果是查询，则工具查询数据，
        3. 如果是用户新增需求 需要再和用户确认后才可以添加
        4. 数据整理成Json进行上下文交互
        """)
    @UserMessage("{{query}}")
    @Agent(name = "xtpQuery", description = "XTP 系统数据查询助手，能够查询制造、工单、采购、库存等制造闭环数据")
    String getData(@V("query") String query);
}
