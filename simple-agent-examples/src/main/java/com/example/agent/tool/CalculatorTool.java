package com.example.agent.tool;

import com.example.agent.tool.anno.Tool;
import com.example.agent.tool.anno.ToolDescription;

/**
 * 提供给工具 Agent 示例使用的简单计算能力。
 */
@Tool
public class CalculatorTool {

    /**
     * 计算两个整数之和。
     *
     * @param a 第一个加数
     * @param b 第二个加数
     * @return 两个整数的和
     */
    @ToolDescription("计算两个整数之和；a 和 b 是两个加数")
    public Integer add(Integer a, Integer b) {
        return a + b;
    }
}
