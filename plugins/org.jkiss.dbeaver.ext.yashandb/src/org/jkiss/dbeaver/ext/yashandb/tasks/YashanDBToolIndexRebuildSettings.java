package org.jkiss.dbeaver.ext.yashandb.tasks;

import org.jkiss.dbeaver.ext.yashandb.model.YashanDBTableIndex;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

/**
 * @Description:
 * @Author dengqh
 * @Date 2023/6/30 19:22
 */
public class YashanDBToolIndexRebuildSettings extends SQLToolExecuteSettings<YashanDBTableIndex> {
    String partition;
    String tablespace;
    Integer initrans;
    Integer pctfree;
    boolean online;

    @Property(viewable = true, editable = true, updatable = true)
    public String getPartition() {
        return partition;
    }

    public void setPartition(String partition) {
        this.partition = partition;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public String getTablespace() {
        return tablespace;
    }

    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public Integer getInitrans() {
        return initrans;
    }

    public void setInitrans(int initrans) {
        this.initrans = initrans;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public Integer getPctfree() {
        return pctfree;
    }

    public void setPctfree(int pctfree) {
        this.pctfree = pctfree;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        partition = JSONUtils.getString(config, "partition");
        tablespace = JSONUtils.getString(config, "tablespace");
        initrans = JSONUtils.getInteger(config, "initrans");
        pctfree = JSONUtils.getInteger(config, "pctfree");
        online = JSONUtils.getBoolean(config, "online");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("partition", getPartition());
        config.put("tablespace",getTablespace());
        config.put("initrans",getInitrans());
        config.put("pctfree",getPctfree());
        config.put("online",isOnline());
    }
}
