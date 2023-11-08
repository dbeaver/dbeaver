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
package org.jkiss.dbeaver.ext.postgresql.model.impls.materialize;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

public class MaterializeSchemaCache extends PostgreDatabase.SchemaCache {
    @Override
    protected MaterializeSchema fetchObject(@NotNull JDBCSession session, @NotNull PostgreDatabase owner,
            @NotNull JDBCResultSet resultSet) throws SQLException {
        String name = JDBCUtils.safeGetString(resultSet, "nspname");
        if (name == null) {
            return null;
        }
        if (MaterializeSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().getNavigatorSettings().isShowUtilityObjects()) {
            return null;
        }
            
        return new MaterializeSchema(owner, name, resultSet);
    }
}
