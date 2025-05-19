package com.liyang.dao;

import com.yxkj.ptjk.netcore.core.BaseDao;
import com.yxkj.ptjk.netcore.core.YxSqlPlus;
import com.yxkj.ptjk.netcore.quartz.entity.TbScheduledTaskConfig;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * TbScheduledTaskConfig 类的简要描述
 *
 * @author liyang
 * @since 2025/5/19
 */
@Repository
public class TbScheduledTaskConfigDao extends BaseDao {

    public List<TbScheduledTaskConfig> queryAll() {
        YxSqlPlus<TbScheduledTaskConfig> yxSqlPlus = new YxSqlPlus<>(TbScheduledTaskConfig.class);
        yxSqlPlus.select();
        return selectList(yxSqlPlus);
    }

    public void insert(TbScheduledTaskConfig config) {
        YxSqlPlus<TbScheduledTaskConfig> yxSqlPlus = new YxSqlPlus<>(TbScheduledTaskConfig.class);
        yxSqlPlus.insert(config);
        insert(yxSqlPlus);
    }

    public void update(TbScheduledTaskConfig config) {
        YxSqlPlus<TbScheduledTaskConfig> yxSqlPlus = new YxSqlPlus<>(TbScheduledTaskConfig.class);
        yxSqlPlus.updateById(config);
        update(yxSqlPlus);
    }

    public void delete(TbScheduledTaskConfig config) {
        YxSqlPlus<TbScheduledTaskConfig> yxSqlPlus = new YxSqlPlus<>(TbScheduledTaskConfig.class);
        yxSqlPlus.deleteById(config);
        delete(yxSqlPlus, 1);
    }

    public TbScheduledTaskConfig getOneByName(String taskId) {
        YxSqlPlus<TbScheduledTaskConfig> yxSqlPlus = new YxSqlPlus<>(TbScheduledTaskConfig.class);
        yxSqlPlus.select().eq(TbScheduledTaskConfig::getTaskId, taskId);
        return select(yxSqlPlus);
    }
}
