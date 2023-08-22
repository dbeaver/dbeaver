/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.tasks.nativetool;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSStructContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Iterator;


public abstract class AbstractImportExportSettings<BASE_OBJECT extends DBSObject> extends AbstractNativeToolSettings<BASE_OBJECT> {
    private static final Log log = Log.getLog(AbstractImportExportSettings.class);

    private String outputFolderPattern;
    private String outputFilePattern;

    public String getOutputFolderPattern() {
        return outputFolderPattern;
    }

    public void setOutputFolderPattern(String outputFolderPattern) {
        this.outputFolderPattern = outputFolderPattern;
    }

    public String getOutputFilePattern() {
        return outputFilePattern;
    }

    public void setOutputFilePattern(String outputFilePattern) {
        this.outputFilePattern = outputFilePattern;
    }

    public void fillExportObjectsFromInput() {

    }

    @Override
    public void loadSettings(DBRRunnableContext runnableContext, DBPPreferenceStore store) throws DBException {
        super.loadSettings(runnableContext, store);
        this.outputFilePattern = store.getString("export.outputFilePattern");
        if (CommonUtils.isEmpty(this.outputFilePattern)) {
            this.outputFilePattern = "dump-${database}-${timestamp}.sql";
        }
        this.outputFolderPattern = CommonUtils.toString(store.getString("export.outputFolder"));
        if (CommonUtils.isEmpty(this.outputFolderPattern)) {
            this.outputFolderPattern = RuntimeUtils.getUserHomeDir().getAbsolutePath();
        }
    }

    @Override
    public void saveSettings(DBRRunnableContext runnableContext, DBPPreferenceStore preferenceStore) {
        super.saveSettings(runnableContext, preferenceStore);
        preferenceStore.setValue("export.outputFilePattern", this.outputFilePattern);
        preferenceStore.setValue("export.outputFolder", this.outputFolderPattern);
    }

    protected String resolveVars(@NotNull DBSObjectContainer container, Collection<? extends DBSSchema> schemas, Collection<? extends DBSTable> tables, String pattern) {
        return GeneralUtils.replaceVariables(pattern, name -> {
            switch (name) {
                case NativeToolUtils.VARIABLE_DATABASE:
                    return container.getName();
                case NativeToolUtils.VARIABLE_HOST:
                    return container.getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case NativeToolUtils.VARIABLE_CONN_TYPE:
                    return container.getDataSource().getContainer().getConnectionConfiguration().getConnectionType().getId();
                case NativeToolUtils.VARIABLE_SCHEMA: {
                    final Iterator<? extends DBSSchema> iterator = schemas == null ? null : schemas.iterator();
                    if (iterator != null && iterator.hasNext()) {
                        return iterator.next().getName();
                    } else {
                        return container instanceof DBSStructContainer ? container.getName() : "null";
                    }
                }
                case NativeToolUtils.VARIABLE_TABLE: {
                    final Iterator<? extends DBSTable> iterator = tables == null ? null : tables.iterator();
                    if (iterator != null && iterator.hasNext()) {
                        return iterator.next().getName();
                    } else {
                        return "null";
                    }
                }
                case NativeToolUtils.VARIABLE_TIMESTAMP:
                    return RuntimeUtils.getCurrentTimeStamp();
                case NativeToolUtils.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    return NativeToolUtils.replaceVariables(name);
            }
        });
    }

}
