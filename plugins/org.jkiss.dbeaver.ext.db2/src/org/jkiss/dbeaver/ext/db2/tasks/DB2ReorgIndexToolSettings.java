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

public class DB2ReorgIndexToolSettings extends SQLToolExecuteSettings<DB2TableBase> {
    private final static String[] tableAccesses = new String[] {"", " ALLOW NO ACCESS", " ALLOW READ ACCESS", " ALLOW WRITE ACCESS"}; //$NON-NLS-1$
    private final static String[] cleanupOptions = new String[] {"", " CLEANUP ALL", " CLEANUP PAGES"}; //$NON-NLS-1$
    private String tableAccess;
    private String cleanupOption;

    @Property(viewable = true, editable = true, updatable = true, order = 1, listProvider = DB2ReorgIndexToolSettings.CheckStorageOptionListProvider.class)
    public String getTableAccess() {
        if (tableAccess == null) {
            tableAccess = tableAccesses[0];
        }
        return tableAccess;
    }

    public void setTableAccess(String tableAccess) {
        this.tableAccess = tableAccess;
    }

    @Property(viewable = true, editable = true, updatable = true, order = 2, listProvider = DB2ReorgIndexToolSettings.CheckTriggersOptionListProvider.class)
    public String getCleanupOption() {
        if (cleanupOption == null) {
            cleanupOption = cleanupOptions[0];
        }
        return cleanupOption;
    }

    public void setCleanupOption(String storageOption) {
        this.cleanupOption = storageOption;
    }

    @Override
    public void loadConfiguration(DBRRunnableContext runnableContext, Map<String, Object> config) {
        super.loadConfiguration(runnableContext, config);
        tableAccess = JSONUtils.getString(config, "table_access"); //$NON-NLS-1$
        cleanupOption = JSONUtils.getString(config, "option"); //$NON-NLS-1$
    }

    @Override
    public void saveConfiguration(Map<String, Object> config) {
        super.saveConfiguration(config);
        config.put("table_access", tableAccess); //$NON-NLS-1$
        config.put("option", cleanupOption); //$NON-NLS-1$
    }
    
    public static class CheckStorageOptionListProvider implements IPropertyValueListProvider<DB2ReorgIndexToolSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2ReorgIndexToolSettings object) {
            return tableAccesses;
        }
    }

    public static class CheckTriggersOptionListProvider implements IPropertyValueListProvider<DB2ReorgIndexToolSettings> {

        @Override
        public boolean allowCustomValue() {
            return false;
        }

        @Override
        public Object[] getPossibleValues(DB2ReorgIndexToolSettings object) {
            return cleanupOptions;
        }
    }

}
