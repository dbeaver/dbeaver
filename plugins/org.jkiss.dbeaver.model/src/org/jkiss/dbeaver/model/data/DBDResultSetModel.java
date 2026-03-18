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
package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.List;

/**
 * Keeps information about entire resultset
 */
public interface DBDResultSetModel {

    @NotNull
    DBDAttributeBinding[] getAttributes() throws DBException;

    @NotNull
    List<DBDAttributeBinding> getVisibleAttributes() throws DBException;

    @NotNull
    List<? extends DBDValueRow> getAllRows();

    @Nullable
    DBDRowIdentifier getDefaultRowIdentifier();

    @Nullable
    Object getCellValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull DBDValueRow row,
        @Nullable int[] rowIndexes,
        boolean retrieveDeepestCollectionElement
    ) throws DBException;

    @Nullable
    Object getCellValue(@NotNull DBDAttributeBinding attribute, @NotNull DBDValueRow row) throws DBException;

    @Nullable
    DBDValueHintContext getHintContext();

    @Nullable
    String getReadOnlyStatus(@Nullable DBPDataSourceContainer dataSourceContainer);

    /**
     * Returns single source of this result set. Usually it is a table.
     * If result set is a result of joins or contains synthetic attributes then
     * single source is null. If driver doesn't support meta information
     * for queries then is will null.
     *
     * @return single source entity
     */
    @Nullable
    DBSEntity getSingleSource() throws DBException;

}