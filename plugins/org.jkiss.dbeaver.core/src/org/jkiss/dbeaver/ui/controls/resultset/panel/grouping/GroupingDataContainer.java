/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;

public class GroupingDataContainer implements DBSDataContainer {

    private IResultSetController parentController;

    public GroupingDataContainer(IResultSetController parentController) {
        this.parentController = parentController;
    }

    @Override
    public DBSObject getParentObject() {
        return parentController.getDataContainer();
    }

    @Override
    public String getName() {
        return "Grouping";
    }

    @Override
    public String getDescription() {
        return "Grouping data";
    }

    @Override
    public DBPDataSource getDataSource() {
        return parentController.getDataContainer().getDataSource();
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT;
    }

    @Override
    public DBCStatistics readData(DBCExecutionSource source, DBCSession session, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException {
        return null;
    }

    @Override
    public long countData(DBCExecutionSource source, DBCSession session, DBDDataFilter dataFilter) throws DBCException {
        return 0;
    }

    @Override
    public boolean isPersisted() {
        return false;
    }
}
