package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class MySQLToolTableRepairSettings extends SQLToolExecuteSettings<MySQLTableBase> {
    private boolean isQuick;
    private boolean isExtended;
    private boolean useFRM;

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isQuick() {
        return isQuick;
    }

    public void setQuick(boolean quick) {
        isQuick = quick;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isExtended() {
        return isExtended;
    }

    public void setExtended(boolean extended) {
        isExtended = extended;
    }

    @Property(viewable = true, editable = true, updatable = true)
    public boolean isUseFRM() {
        return useFRM;
    }

    public void setUseFRM(boolean useFRM) {
        this.useFRM = useFRM;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        isQuick = JSONUtils.getBoolean(config, "QUICK");
        isExtended = JSONUtils.getBoolean(config, "EXTENDED");
        useFRM = JSONUtils.getBoolean(config, "USE_FRM");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("QUICK", isQuick);
        config.put("EXTENDED", isExtended);
        config.put("USE_FRM", useFRM);
    }
}
