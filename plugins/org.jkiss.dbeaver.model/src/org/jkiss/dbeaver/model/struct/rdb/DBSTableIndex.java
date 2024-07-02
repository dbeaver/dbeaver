/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.struct.rdb;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.List;

/**
 * DBSTableIndex
 */
public interface DBSTableIndex extends DBSEntityConstraint, DBSEntityReferrer, DBPQualifiedObject
{
    /**
     * Index container. In complex databases it is schema or catalog where index defined.
     * Also the table can be index container.
     * @return container
     */
    DBSObject getContainer();

    DBSEntity getTable();

    boolean isUnique();

    boolean isPrimary();

    DBSIndexType getIndexType();

    List<? extends DBSTableIndexColumn> getAttributeReferences(@NotNull DBRProgressMonitor monitor)
        throws DBException;

}
