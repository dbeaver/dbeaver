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

package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDValueMeta;

/**
 * DBCResultSet
 */
public interface DBCResultSet extends DBPObject, DBPCloseableObject
{
    String FEATURE_NAME_JDBC            = "jdbc";
    String FEATURE_NAME_DOCUMENT        = "document";
    String FEATURE_NAME_LOCAL           = "local";

    DBCSession getSession();

    DBCStatement getSourceStatement();

    /**
     * Gets attribute value
     * @param index    index (zero-based)
     * @return         value (nullable)
     * @throws DBCException
     */
    @Nullable
    Object getAttributeValue(int index) throws DBCException;

    @Nullable
    Object getAttributeValue(String name) throws DBCException;

    @Nullable
    DBDValueMeta getAttributeValueMeta(int index) throws DBCException;

    @Nullable
    DBDValueMeta getRowMeta() throws DBCException;

    boolean nextRow() throws DBCException;

    boolean moveTo(int position) throws DBCException;

    @NotNull
    DBCResultSetMetaData getMeta() throws DBCException;

    @Nullable
    String getResultSetName() throws DBCException;

    @Nullable
    Object getFeature(String name);
}
