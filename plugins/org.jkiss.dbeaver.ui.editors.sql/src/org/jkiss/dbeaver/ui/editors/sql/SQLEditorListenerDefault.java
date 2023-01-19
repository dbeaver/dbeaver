/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener.PreferenceChangeEvent;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLQueryResult;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * SQLEditorListenerDefault
 */
public class SQLEditorListenerDefault implements SQLEditorListener
{
    @Override
    public void onConnect(DBPDataSourceContainer container) {

    }

    @Override
    public void onDisconnect(DBPDataSourceContainer container) {

    }

    @Override
    public void beforeQueryExecute(boolean script, boolean newTabs) {

    }

    @Override
    public void afterQueryExecute(boolean script, boolean newTabs) {

    }

    @Override
    public void onQueryChange(SQLQuery oldQuery, SQLQuery newQuery) {

    }

    @Override
    public void beforeQueryPlanExplain() {

    }

    @Override
    public void onDataSourceChanged(PreferenceChangeEvent event) {
        
    }
    
    @Override
    public void onDataReceived(@NotNull DBPPreferenceStore contextPrefStore, @NotNull ResultSetModel resultSet, @Nullable String name) {

    }

    @Override
    public void onQueryResult(@NotNull DBPPreferenceStore contextPrefStore, @NotNull SQLQueryResult result) {

    }
}
