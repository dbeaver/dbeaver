/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;

/**
 * Data container.
 * Provides facilities to query object for data.
 * Any data container MUST support data read. Other function may be not supported (client can check it with {@link #getSupportedFeatures()}).
 */
public interface DBSDataContainer extends DBSObject {

    public static final int DATA_SELECT         = 0;
    public static final int DATA_COUNT          = 1;
    public static final int DATA_FILTER         = 2 << 1;
    public static final int DATA_SEARCH         = 4 << 2;

    public static final long FLAG_NONE               = 0;
    public static final long FLAG_READ_PSEUDO        = 1 << 1;

    /**
     * Features supported by implementation
     * @return features flags
     */
    int getSupportedFeatures();

    /**
     * Reads data from container and pushes it into receiver
     *
     * @param session execution context
     * @param dataReceiver data receiver. Works as a data pipe
     * @param dataFilter data filter. May be null
     * @param firstRow first row number (<= 0 means do not use it)
     * @param maxRows total rows to fetch (<= 0 means fetch everything)
     * @param flags read flags. See FLAG_ constants
     * @return number of fetched rows
     * @throws DBCException on any error
     */
    @NotNull
    DBCStatistics readData(
        @NotNull DBCSession session,
        @NotNull DBDDataReceiver dataReceiver,
        @Nullable DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        long flags)
        throws DBCException;

    /**
     * Counts data rows in container.
     * @param session execution context
     * @param dataFilter data filter (may be null)
     * @return number of rows in container. May return negative values if count feature is not available
     * @throws DBCException on any error
     */
    long countData(
        @NotNull DBCSession session,
        @Nullable DBDDataFilter dataFilter)
        throws DBCException;

}
