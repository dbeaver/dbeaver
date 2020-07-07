/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.db2.tasks;

import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.meta.IPropertyValueListProvider;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.sql.task.SQLToolExecuteSettings;

import java.util.Map;

public class DB2ToolTableTruncateSettings extends SQLToolExecuteSettings<DB2TableBase> {
    private final static String[] storageOptions = new String[] {"DROP STORAGE", "REUSE STORAGE"};
    private final static String[] triggerOptions = new String[] {"IGNORE DELETE TRIGGERS", "RESTRICT WHEN DELETE TRIGGERS"};
    private String storageOption;
    private String triggerOption;

    @Property(viewable = true, editable = true, updatable = true, order = 1, listProvider = CheckStorageOptionListProvider.class)
    public String getStorageOption() {
        if (storageOption == null) {
            storageOption = storageOptions[0];
        }
        return storageOption;
    }

    public void setStorageOption(String storageOption) {
        this.storageOption = storageOption;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2, listProvider = CheckTriggersOptionListProvider.class)
    public String getTriggerOption() {
        if (triggerOption == null) {
            triggerOption= triggerOptions[0];
        }
        return triggerOption;
    }

    public void setTriggerOption(String storageOption) {
        this.triggerOption = storageOption;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        storageOption = JSONUtils.getString(config, "storage_option");
        triggerOption = JSONUtils.getString(config, "trigger_option");
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("storage_option", storageOption);
        config.put("trigger_option", triggerOption);
    }

    public static class CheckStorageOptionListProvider implements IPropertyValueListProvider<DB2ToolTableTruncateSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2ToolTableTruncateSettings object) {
            return storageOptions;
        }
    }

    public static class CheckTriggersOptionListProvider implements IPropertyValueListProvider<DB2ToolTableTruncateSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2ToolTableTruncateSettings object) {
            return triggerOptions;
        }
    }
}
