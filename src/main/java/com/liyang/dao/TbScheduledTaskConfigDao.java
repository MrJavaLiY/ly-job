package com.liyang.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liyang.entity.TbScheduledTaskConfig;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TbScheduledTaskConfig 类的简要描述
 *
 * @author liyang
 * @since 2025/5/19
 */
@Repository
public interface TbScheduledTaskConfigDao extends BaseMapper<TbScheduledTaskConfig> {


    default TbScheduledTaskConfig getOneByName(String taskId) {
        LambdaQueryWrapper<TbScheduledTaskConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TbScheduledTaskConfig::getTaskId, taskId);
        return selectOne(wrapper);
    }

    default boolean isTableExists() {
        try {
            // 查询 LIMIT 1 的记录，不真正取数据，只验证表是否存在
            selectObjs(new LambdaQueryWrapper<TbScheduledTaskConfig>().last("LIMIT 1"));
            return true;
        } catch (BadSqlGrammarException e) {
            // 捕获 SQL语法错误，通常是表不存在
            return false;
        }
    }
}
