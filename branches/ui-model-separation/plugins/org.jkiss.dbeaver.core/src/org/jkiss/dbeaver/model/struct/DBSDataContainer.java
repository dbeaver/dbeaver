/*
 * Copyright (C) 2010-2012 Serge Rieder
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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;

import java.util.List;

/**
 * Data container.
 * Provides facilities to query object for data.
 * Any data container MUST support data read. Other function may be not supported (client can check it with {@link #getSupportedFeatures()}).
 */
public interface DBSDataContainer extends DBSObject {

    public static final int DATA_SELECT         = 0;
    public static final int DATA_COUNT          = 1;
    public static final int DATA_INSERT         = 2;
    public static final int DATA_UPDATE         = 4;
    public static final int DATA_DELETE         = 8;
    public static final int DATA_FILTER         = 16;

    int getSupportedFeatures();

    long readData(
        DBCExecutionContext context,
        DBDDataReceiver dataReceiver,
        DBDDataFilter dataFilter,
        long firstRow,
        long maxRows)
        throws DBException;

    long readDataCount(
        DBCExecutionContext context,
        DBDDataFilter dataFilter)
        throws DBException;

    long insertData(
        DBCExecutionContext context,
        List<DBDAttributeValue> attributes,
        DBDDataReceiver keysReceiver)
        throws DBException;

    long updateData(
        DBCExecutionContext context,
        List<DBDAttributeValue> keyAttributes,
        List<DBDAttributeValue> updateAttributes,
        DBDDataReceiver keysReceiver)
        throws DBException;

    long deleteData(
        DBCExecutionContext context,
        List<DBDAttributeValue> keyAttributes)
        throws DBException;

}
