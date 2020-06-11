package org.jkiss.dbeaver.ext.oracle.tasks;

import org.jkiss.dbeaver.ext.oracle.model.OracleTableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class OracleToolTableValidateStructureSettings extends SQLToolExecuteSettings<OracleTableBase> {
    private String option;

    @Property(viewable = true, editable = true, updatable = true, listProvider = CheckOptionListProvider.class)
    public String getOption() {
        return option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        option = JSONUtils.getString(config, "option");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("option", option);
    }

    public static class CheckOptionListProvider implements IPropertyValueListProvider<OracleToolTableValidateStructureSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(OracleToolTableValidateStructureSettings object) {
            return new String[] {
                    "",
                    "CASCADE",
                    "CASCADE FAST",
                    "CASCADE ONLINE",
            };
        }
    }
}
