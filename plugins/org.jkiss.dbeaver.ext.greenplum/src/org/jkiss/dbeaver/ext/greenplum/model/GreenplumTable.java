/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableColumn;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * GreenplumTable
 */
public class GreenplumTable extends PostgreTableRegular {

    private static final Log log = Log.getLog(GreenplumTable.class);

    private int[] distributionColumns;

    private boolean supportsReplicatedDistribution = false;

    public GreenplumTable(PostgreSchema catalog) {
        super(catalog);
    }

    public GreenplumTable(PostgreSchema catalog, ResultSet dbResult) {
        super(catalog, dbResult);

        if (catalog.getDataSource().isServerVersionAtLeast(9, 1)) {
            supportsReplicatedDistribution = true;
        }
    }

    private List<PostgreTableColumn> getDistributionPolicy(DBRProgressMonitor monitor) throws DBException {
        if (distributionColumns == null) {
            try {
                distributionColumns = GreenplumUtils.readDistributedColumns(monitor, this);
            } catch (Throwable e) {
                log.error("Error reading distribution policy", e);
            }
            if (distributionColumns == null) {
                distributionColumns = new int[0];
            }
        }

        if (distributionColumns.length == 0) {
            return null;
        }
        List<PostgreTableColumn> columns = new ArrayList<>(distributionColumns.length);
        for (int i = 0; i < distributionColumns.length; i++) {
            PostgreTableColumn attr = getAttributeByPos(monitor, distributionColumns[i]);
            if (attr == null) {
                log.debug("Bad policy attribute position: " + distributionColumns[i]);
            } else {
                columns.add(attr);
            }
        }
        return columns;
    }

    @Override
    public void appendTableModifiers(DBRProgressMonitor monitor, StringBuilder ddl) {
        try {
            List<PostgreTableColumn> distributionColumns = getDistributionPolicy(monitor);
            if (CommonUtils.isEmpty(distributionColumns)) {
                distributionColumns = GreenplumUtils.getDistributionTableColumns(monitor, distributionColumns, this);
            }

            GreenplumUtils.addObjectModifiersToDDL(monitor, ddl, this, distributionColumns, supportsReplicatedDistribution);
        } catch (DBException e) {
            log.error("Error reading Greenplum table properties", e);
        }
    }

}
