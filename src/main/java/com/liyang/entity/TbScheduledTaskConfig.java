package com.liyang.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yxkj.ptjk.netcore.autotable.annotation.CreateTable;
import com.yxkj.ptjk.netcore.autotable.annotation.TableColumn;
import lombok.Data;

import java.util.Date;

/**
 * TbScheduledTaskConfig 类的简要描述
 *
 * @author liyang
 * @since 2025/5/19
 */
@Data
@TableName("tb_scheduled_task_config")
@CreateTable
public class TbScheduledTaskConfig {
    @TableId("task_Id")
    @TableColumn(description = "主键,任务id")
    private String taskId;

    @TableField("task_name")
    @TableColumn(description = "任务名称,中文")
    private String taskName;

    @TableField("task_bean")
    @TableColumn(description = "任务bean，禁止修改")
    private String taskBean;

    @TableField("task_method")
    @TableColumn(description = "任务方法，禁止修改")
    private String taskMethod;

    @TableField("enabled")
    @TableColumn(description = "是否启用，0关1开")
    private Boolean enabled;

    @TableField("cron_expression")
    @TableColumn(description = "定时任务cron表达式")
    private String cronExpression;

    @TableField("fixed_rate")
    @TableColumn(description = "定时任务固定频率")
    private Long fixedRate;

    @TableField("fixed_delay")
    @TableColumn(description = "定时任务固定延迟")
    private Long fixedDelay;

    @TableField("initial_delay")
    @TableColumn(description = "定时任务初始延迟")
    private Long initialDelay;

    @TableField("last_modified_time")
    @TableColumn(description = "最后修改时间")
    private Date lastModifiedTime;
}
