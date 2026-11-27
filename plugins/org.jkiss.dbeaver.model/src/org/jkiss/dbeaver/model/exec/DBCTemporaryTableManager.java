/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

public interface DBCTemporaryTableManager {
    @Nullable
    TemporaryTable registerTemporaryTable(@NotNull DBCSession session, @NotNull List<String> qualifiedName, boolean ifNotExists)
        throws DBException;

    @Nullable
    TemporaryTable temporaryTableChanged(
        @NotNull DBCSession session, @NotNull List<String> qualifiedName, boolean dropped, @NotNull List<String> newName
    )
        throws DBException;

    @Nullable
    TemporaryTable findTemporaryTable(@NotNull DBCEntityMetaData metadata);

    interface TemporaryTable extends DBSEntity {
        /** Whether this metadata still represents the table in its original physical session. */
        default boolean isCurrentTemporaryTable() {
            return true;
        }

        boolean refreshTemporaryMetadata(@NotNull DBRProgressMonitor monitor) throws DBException;
    }
}
