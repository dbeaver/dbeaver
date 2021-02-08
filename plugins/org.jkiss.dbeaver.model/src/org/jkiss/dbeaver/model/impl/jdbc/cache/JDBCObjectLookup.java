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
package org.jkiss.dbeaver.model.impl.jdbc.cache;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

/**
 * Extension of {@link JDBCObjectCache} - support object lookup by name
 */
public interface JDBCObjectLookup<OWNER extends DBSObject, OBJECT extends DBSObject>
{
    /**
     * Creates statement to read just one object.
     * Parameter @object OR @objectName may be specified to find an object
     */
    @NotNull
    JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull OWNER owner, @Nullable OBJECT object, @Nullable String objectName)
        throws SQLException;

}
