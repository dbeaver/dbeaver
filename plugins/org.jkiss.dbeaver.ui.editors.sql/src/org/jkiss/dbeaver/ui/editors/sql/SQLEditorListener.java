/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.sql.SQLQuery;

/**
 * SQLEditorListener
 *
 * @author Serge Rider
 */
public interface SQLEditorListener
{
    void onConnect(DBPDataSourceContainer container);

    void onDisconnect(DBPDataSourceContainer container);

    void beforeQueryExecute(boolean script, boolean newTabs);

    void afterQueryExecute(boolean script, boolean newTabs);

    void onQueryChange(SQLQuery oldQuery, SQLQuery newQuery);

    void beforeQueryPlanExplain();
}
