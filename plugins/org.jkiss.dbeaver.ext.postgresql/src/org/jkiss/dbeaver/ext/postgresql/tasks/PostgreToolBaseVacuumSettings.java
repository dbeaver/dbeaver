package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Map;

public class PostgreToolBaseVacuumSettings extends SQLToolExecuteSettings<DBSObject> {
    private boolean isFull;
    private boolean isFreeze;
    private boolean isAnalyzed;
    private boolean isDisableSkipping;

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isFreeze() {
        return isFreeze;
    }

    public void setFreeze(boolean freeze) {
        isFreeze = freeze;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isAnalyzed() {
        return isAnalyzed;
    }

    public void setAnalyzed(boolean analyzed) {
        isAnalyzed = analyzed;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isDisableSkipping() {
        return isDisableSkipping;
    }

    public void setDisableSkipping(boolean disableSkipping) {
        isDisableSkipping = disableSkipping;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        isFull = JSONUtils.getBoolean(config, "full");
        isFreeze = JSONUtils.getBoolean(config, "freeze");
        isAnalyzed = JSONUtils.getBoolean(config, "analyze");
        isDisableSkipping = JSONUtils.getBoolean(config, "disable_page_skipping");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("full", isFull);
        config.put("freeze", isFreeze);
        config.put("analyze", isAnalyzed);
        config.put("disable_page_skipping", isDisableSkipping);
    }
}
