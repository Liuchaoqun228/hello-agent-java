package com.example.agent;

import com.example.agent.tool.anno.Tool;
import com.example.agent.tool.anno.ToolDescription;

@Tool
public class CommonTool {

    @ToolDescription("计算两个整数之和；入参为 a、b，出参为计算结果")
    public Integer add(Integer a, Integer b) {
        // 直接使用类型化参数实现业务逻辑，不再感知模型参数 Map。
        return a + b;
    }
}
