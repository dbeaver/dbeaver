/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.model.exec;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDValueMeta;

/**
 * DBCResultSet
 */
public interface DBCResultSet extends DBPObject
{
    DBCSession getSession();

    DBCStatement getSourceStatement();

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
    DBCResultSetMetaData getResultSetMetaData() throws DBCException;

    @Nullable
    String getResultSetName() throws DBCException;

    void close();
}
