/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
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

}
