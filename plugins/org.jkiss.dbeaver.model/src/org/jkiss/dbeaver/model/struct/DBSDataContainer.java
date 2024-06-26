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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.dpi.DPIObject;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.utils.ArrayUtils;

/**
 * Data container.
 * Provides facilities to query object for data.
 * Any data container MUST support data read. Other function may be not supported (client can check it with {@link #getSupportedFeatures()}).
 */
@DPIObject
public interface DBSDataContainer extends DBSObject {

    String FEATURE_DATA_SELECT = "data.select";
    String FEATURE_DATA_COUNT = "data.count";
    String FEATURE_DATA_FILTER = "data.filter";
    String FEATURE_DATA_SEARCH = "data.search";
    String FEATURE_KEY_VALUE = "data.key.value";
    String FEATURE_DATA_MODIFIED_ON_REFRESH = "data.modifying";

    long FLAG_NONE                  = 0;
    long FLAG_READ_PSEUDO           = 1 << 1;
    long FLAG_USE_SELECTED_ROWS     = 1 << 2;
    long FLAG_USE_SELECTED_COLUMNS  = 1 << 3;
    long FLAG_FETCH_SEGMENT         = 1 << 4;
    long FLAG_REFRESH               = 1 << 8;

    DBPDataSource getDataSource();

    /**
     * Features supported by implementation
     * @return supported features
     */
    String[] getSupportedFeatures();

    default boolean isFeatureSupported(String feature) {
        return ArrayUtils.contains(getSupportedFeatures(), feature);
    }

    /**
     * Reads data from container and pushes it into receiver
     *
     * @param source       source
     * @param session source
     * @param dataReceiver data receiver. Works as a data pipe
     * @param dataFilter data filter. May be null
     * @param firstRow first row number (<= 0 means do not use it)
     * @param maxRows total rows to fetch (<= 0 means fetch everything)
     * @param flags read flags. See FLAG_ constants
     * @param fetchSize fetch size
     * @return number of fetched rows
     * @throws DBCException on any error
     */
    @NotNull
    DBCStatistics readData(
        @Nullable DBCExecutionSource source,
        @NotNull DBCSession session,
        @NotNull DBDDataReceiver dataReceiver,
        @Nullable DBDDataFilter dataFilter,
        long firstRow,
        long maxRows,
        long flags,
        int fetchSize)
        throws DBCException;

    /**
     * Counts data rows in container.
     *
     * @param source execution source
     * @param session session
     * @param dataFilter data filter (may be null)
     * @param flags read flags. See FLAG_ constants
     * @return number of rows in container. May return negative values if count feature is not available
     * @throws DBCException on any error
     */
    long countData(
        @NotNull DBCExecutionSource source,
        @NotNull DBCSession session,
        @Nullable DBDDataFilter dataFilter,
        long flags)
        throws DBCException;

}
