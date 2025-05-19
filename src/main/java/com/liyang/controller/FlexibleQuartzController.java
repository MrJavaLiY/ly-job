package com.liyang.controller;

import com.yxkj.ptjk.netcore.quartz.entity.TbScheduledTaskConfig;
import com.yxkj.ptjk.netcore.quartz.operation.DynamicTaskManager;
import com.yxkj.ptjk.netcore.utils.ResponseEntity;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * FlexibleQuartzController 类的简要描述
 *
 * @author liyang
 * @since 2025/5/19
 */
@RestController
@RequestMapping("/apis/tasks")
@Slf4j
@Api(tags ="轮询调度")
public class FlexibleQuartzController {
    @Resource
    private DynamicTaskManager taskManager;

    @GetMapping("/all")
    @ApiOperation(value = "获取所有任务状态")
    public Map<String, DynamicTaskManager.TaskStatus> getAllTasks() {
        return taskManager.getAllTaskStatus();
    }

    @PostMapping("/start")
    @ApiOperation(value = "启动任务")
    public ResponseEntity<?> startTask(@RequestParam String taskName) {
        return taskManager.startTask(taskName) ? ResponseEntity.success() : ResponseEntity.failed("启动任务失败");
    }

    @PostMapping("/stop")
    @ApiOperation(value = "停止任务")
    public ResponseEntity<?> stopTask(@RequestParam String taskName) {
        return taskManager.stopTask(taskName) ?
                ResponseEntity.success() : ResponseEntity.failed("停止任务失败");
    }
    @PostMapping("/pause")
    @ApiOperation(value = "暂停任务")
    public ResponseEntity<?> pauseTask(@RequestParam String taskName) {
        return taskManager.pauseTask(taskName) ?
                ResponseEntity.success() : ResponseEntity.failed("暂停任务");
    }

    @PutMapping("/updateTask")
    @ApiOperation(value = "修改任务")
    public ResponseEntity<?> updateTask(
            @RequestBody TbScheduledTaskConfig newConfig) {
        return taskManager.updateTaskConfig(newConfig.getTaskId(), newConfig) ?
                ResponseEntity.success() : ResponseEntity.failed("更新任务配置失败");
    }
}
