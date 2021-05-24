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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.Pair;

/**
 * SQLServerExtendedPropertyOwner
 * <p>
 * Represents entity on which the extended properties can be added.
 *
 * @see <a href="https://docs.microsoft.com/en-us/sql/relational-databases/system-stored-procedures/sp-addextendedproperty-transact-sql">sp_addextendedproperty</a>
 * @see <a href="https://docs.microsoft.com/en-us/sql/relational-databases/system-catalog-views/extended-properties-catalog-views-sys-extended-properties">sys.extended_properties</a>
 */
public interface SQLServerExtendedPropertyOwner extends SQLServerObject {

    /**
     * ID of the item on which the extended property exists, interpreted according to its class.
     */
    long getMajorObjectId();

    /**
     * Secondary ID of the item on which the extended property exists, interpreted according to its class.
     */
    long getMinorObjectId();

    /**
     * Name and the object of the specified {@code level} on which the extended property exists.
     * <p>
     * For example, if the object class is {@link SQLServerObjectClass#OBJECT_OR_COLUMN} and
     * the minor id is {@code > 0}, then following objects may be returned for given {@code level}:
     * <ul>
     * <li>{@code level 0 - Schema}</li>
     * <li>{@code level 1 - Table}</li>
     * <li>{@code level 2 - Column}</li>
     * </ul>
     */
    @Nullable
    Pair<String, SQLServerObject> getExtendedPropertyObject(@NotNull DBRProgressMonitor monitor, int level);

    /**
     * Identifies the class of item on which the property exists.
     */
    @NotNull
    SQLServerObjectClass getExtendedPropertyObjectClass();

    @NotNull
    SQLServerExtendedPropertyCache getExtendedPropertyCache();

    @NotNull
    SQLServerDatabase getDatabase();

}
