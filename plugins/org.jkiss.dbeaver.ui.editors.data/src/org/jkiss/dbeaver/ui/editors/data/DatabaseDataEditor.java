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
package org.jkiss.dbeaver.ui.editors.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.ISmartTransactionManager;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;

/**
 * DatabaseDataEditor
 */
public class DatabaseDataEditor extends AbstractDataEditor<DBSDataContainer> implements ISmartTransactionManager
{
    public static final String ATTR_SUSPEND_QUERY = "suspendQuery";
    public static final String ATTR_DATA_FILTER = "dataFilter";

    @Nullable
    @Override
    public DBSDataContainer getDataContainer()
    {
        return (DBSDataContainer)getEditorInput().getDatabaseObject();
    }

    @Override
    protected DBDDataFilter getEditorDataFilter() {
        return (DBDDataFilter) getEditorInput().getAttribute(ATTR_DATA_FILTER);
    }

    @Override
    protected boolean isSuspendDataQuery() {
        return CommonUtils.toBoolean(getEditorInput().getAttribute(ATTR_SUSPEND_QUERY));
    }

    @Override
    protected String getDataQueryMessage() {
        return "Query data from '" + getEditorInput().getDatabaseObject().getName() + "'...";
    }

    @Override
    public boolean isReadyToRun() {
        return getDatabaseObject() != null && getDatabaseObject().isPersisted();
    }

    @Override
    public boolean isSmartAutoCommit() {
        return getActivePreferenceStore().getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT);
    }

    @Override
    public void setSmartAutoCommit(boolean smartAutoCommit) {
        getActivePreferenceStore().setValue(ModelPreferences.TRANSACTIONS_SMART_COMMIT, smartAutoCommit);
        try {
            getActivePreferenceStore().save();
        } catch (IOException e) {
            DBWorkbench.getPlatformUI().showError("Samrt commit", "Error saving smart auto-commit option", e);
        }
    }

    @NotNull
    private DBPPreferenceStore getActivePreferenceStore() {
        DBPDataSource dataSource = getDatabaseObject().getDataSource();
        if (dataSource == null) {
            return DBWorkbench.getPlatform().getPreferenceStore();
        }
        return dataSource.getContainer().getPreferenceStore();
    }


}
