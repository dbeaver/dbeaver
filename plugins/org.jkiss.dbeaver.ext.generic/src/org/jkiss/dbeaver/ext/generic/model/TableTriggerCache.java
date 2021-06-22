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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectWithParentCache;

import java.sql.SQLException;

public class TableTriggerCache extends JDBCObjectWithParentCache<GenericStructContainer, GenericTableBase, GenericTrigger> {

    TableTriggerCache(TableCache tableCache) {
        super(tableCache, GenericTableBase.class, "OWNER", "TRIGGER_NAME");
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @Nullable GenericTableBase tableBase) throws SQLException {
        return genericStructContainer.getDataSource().getMetaModel().prepareTableTriggersLoadStatement(session, genericStructContainer, tableBase);
    }
    @Nullable
    @Override
    protected GenericTrigger fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer genericStructContainer, @NotNull GenericTableBase genericTableBase, String triggerName, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
        return genericStructContainer.getDataSource().getMetaModel().createTableTriggerImpl(session, genericStructContainer, genericTableBase, triggerName, resultSet);
    }
}
