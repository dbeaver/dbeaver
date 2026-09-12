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
package org.jkiss.dbeaver.ext.doris.edit;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.doris.model.DorisDataSource;
import org.jkiss.dbeaver.ext.generic.edit.GenericTableManager;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Map;

/**
 * Doris table manager.
 */
public class DorisTableManager extends GenericTableManager {

    // Doris uses three replicas when CREATE TABLE omits replication properties.
    private static final int DEFAULT_REPLICA_COUNT = 3;
    private static final String DEFAULT_DISTRIBUTION = "DISTRIBUTED BY RANDOM BUCKETS 1"; //$NON-NLS-1$
    private static final String REPLICATION_PROPERTIES = "PROPERTIES (\"replication_num\" = \"%d\")"; //$NON-NLS-1$

    @Override
    protected void appendTableModifiers(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table,
        @NotNull NestedObjectCommand tableProps,
        @NotNull StringBuilder ddl,
        boolean alter,
        @NotNull Map<String, Object> options
    ) {
        String delimiter = getDelimiter(options);
        ddl.append(delimiter).append(DEFAULT_DISTRIBUTION);
        Integer replicaCount = getReplicaCount(monitor, table);
        if (replicaCount != null) {
            ddl.append(delimiter).append(REPLICATION_PROPERTIES.formatted(replicaCount));
        }
    }

    @Nullable
    private static Integer getReplicaCount(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericTableBase table
    ) {
        int availableBackendCount = ((DorisDataSource) table.getDataSource()).getAvailableBackendCount(monitor);
        if (availableBackendCount > 0 && availableBackendCount < DEFAULT_REPLICA_COUNT) {
            return availableBackendCount;
        }
        return null;
    }
}
