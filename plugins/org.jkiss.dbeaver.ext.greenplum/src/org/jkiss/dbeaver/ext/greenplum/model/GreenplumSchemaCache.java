/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

import java.sql.SQLException;

public class GreenplumSchemaCache extends PostgreDatabase.SchemaCache {

    @Override
    protected GreenplumSchema fetchObject(@NotNull JDBCSession session,
                                          @NotNull PostgreDatabase owner,
                                          @NotNull JDBCResultSet resultSet) throws SQLException {
        String name = JDBCUtils.safeGetString(resultSet, "nspname");
        if (name == null) {
            return null;
        }
        if (GreenplumSchema.isUtilitySchema(name) && !owner.getDataSource().getContainer().isShowUtilityObjects()) {
            return null;
        }
        return new GreenplumSchema(owner, name, resultSet);
    }
}
