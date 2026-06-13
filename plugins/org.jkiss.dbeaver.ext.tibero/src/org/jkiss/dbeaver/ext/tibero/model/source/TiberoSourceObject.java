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
package org.jkiss.dbeaver.ext.tibero.model.source;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.tibero.model.TiberoDataSource;
import org.jkiss.dbeaver.ext.tibero.model.TiberoSchema;
import org.jkiss.dbeaver.model.DBPStatefulObject;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;

public interface TiberoSourceObject extends DBSObject, DBSObjectWithScript, DBPStatefulObject {

    @NotNull
    @Override
    TiberoDataSource getDataSource();

    @Nullable
    TiberoSchema getSchema();

    @NotNull
    TiberoSourceType getSourceType();

    @NotNull
    DBEPersistAction[] getCompileActions(@NotNull DBRProgressMonitor monitor) throws DBCException;

    default boolean supportsCompile() {
        return true;
    }
}
