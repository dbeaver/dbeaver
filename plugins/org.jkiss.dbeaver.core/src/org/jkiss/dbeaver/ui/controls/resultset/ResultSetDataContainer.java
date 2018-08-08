/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IAdaptable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.data.DBDValueMeta;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.local.LocalResultSetMeta;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-side data container.
 * Wraps RSV model and original data container.
 */
public class ResultSetDataContainer implements DBSDataContainer, IAdaptable {

    private static final Log log = Log.getLog(ResultSetDataContainer.class);

    private final DBSDataContainer dataContainer;
    private final ResultSetModel model;
    private ResultSetDataContainerOptions options;

    public ResultSetDataContainer(DBSDataContainer dataContainer, ResultSetModel model, ResultSetDataContainerOptions options) {
        this.dataContainer = dataContainer;
        this.model = model;
        this.options = options;
    }

    @Override
    public String getDescription() {
        return dataContainer.getDescription();
    }

    @Override
    public DBSObject getParentObject() {
        return dataContainer.getParentObject();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataContainer.getDataSource();
    }

    @Override
    public int getSupportedFeatures() {
        return DATA_SELECT | DATA_COUNT;
    }

    public ResultSetDataContainerOptions getOptions() {
        return options;
    }

    @Override
    public DBCStatistics readData(DBCExecutionSource source, DBCSession session, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags) throws DBCException {
        if (proceedSelectedRowsOnly(flags) || proceedSelectedColumnsOnly(flags)) {

            long startTime = System.currentTimeMillis();
            DBCStatistics statistics = new DBCStatistics();
            statistics.setExecuteTime(System.currentTimeMillis() - startTime);

            //LocalSta
            ModelResultSet resultSet = new ModelResultSet(session, flags);
            long resultCount = 0;
            try {
                dataReceiver.fetchStart(session, resultSet, firstRow, maxRows);
                while (resultSet.nextRow()) {
                    if (!proceedSelectedRowsOnly(flags) || options.getSelectedRows().contains(resultCount)) {
                        dataReceiver.fetchRow(session, resultSet);
                    }
                    resultCount++;
                }
            } finally {
                try {
                    dataReceiver.fetchEnd(session, resultSet);
                } catch (DBCException e) {
                    log.error("Error while finishing result set fetch", e); //$NON-NLS-1$
                }
                resultSet.close();
                dataReceiver.close();
            }
            statistics.setFetchTime(System.currentTimeMillis() - startTime);
            statistics.setRowsFetched(resultCount);
            return statistics;
        } else {
            return dataContainer.readData(source, session, dataReceiver, dataFilter, firstRow, maxRows, flags);
        }
    }

    private boolean proceedSelectedColumnsOnly(long flags) {
        return (flags & DBSDataContainer.FLAG_USE_SELECTED_COLUMNS) != 0 && !CommonUtils.isEmpty(options.getSelectedColumns());
    }

    private boolean proceedSelectedRowsOnly(long flags) {
        return (flags & DBSDataContainer.FLAG_USE_SELECTED_ROWS) != 0 && !CommonUtils.isEmpty(options.getSelectedRows());
    }

    @Override
    public long countData(DBCExecutionSource source, DBCSession session, DBDDataFilter dataFilter, long flags) throws DBCException {
        if (proceedSelectedRowsOnly(flags)) {
            return options.getSelectedRows().size();
        } else if (proceedSelectedColumnsOnly(flags)) {
            return model.getRowCount();
        } else {
            return dataContainer.countData(source, session, dataFilter, flags);
        }
    }

    @Override
    public String getName() {
        return dataContainer.getName();
    }

    @Override
    public boolean isPersisted() {
        return dataContainer.isPersisted();
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isInstance(dataContainer)) {
            return adapter.cast(dataContainer);
        }
        return null;
    }

    private class ModelResultSet implements DBCResultSet {

        private final DBCSession session;
        private final long flags;
        private ResultSetRow curRow;

        ModelResultSet(DBCSession session, long flags) {
            this.session = session;
            this.flags = flags;
        }

        @Override
        public DBCSession getSession() {
            return session;
        }

        @Override
        public DBCStatement getSourceStatement() {
            return null;
        }

        @Override
        public Object getAttributeValue(int index) throws DBCException {
            return model.getCellValue(model.getVisibleAttribute(index), curRow);
        }

        @Override
        public Object getAttributeValue(String name) throws DBCException {
            DBDAttributeBinding attr = DBUtils.findObject(model.getVisibleAttributes(), name);
            if (attr == null) {
                throw new DBCException("Attribute '" + name + "' not found");
            }
            return model.getCellValue(attr, curRow);
        }

        @Override
        public DBDValueMeta getAttributeValueMeta(int index) throws DBCException {
            return null;
        }

        @Override
        public DBDValueMeta getRowMeta() throws DBCException {
            return null;
        }

        @Override
        public boolean nextRow() throws DBCException {
            if (curRow == null) {
                if (model.getRowCount() == 0) {
                    return false;
                }
                curRow = model.getRow(0);
            } else {
                if (curRow.getRowNumber() >= model.getRowCount() - 1) {
                    return false;
                }
                curRow = model.getRow(curRow.getRowNumber() + 1);
            }
            return true;
        }

        @Override
        public boolean moveTo(int position) throws DBCException {
            if (position >= model.getRowCount() - 1) {
                return false;
            }
            curRow = model.getRow(position);
            return true;
        }

        @Override
        public DBCResultSetMetaData getMeta() throws DBCException {
            List<DBDAttributeBinding> attributes = model.getVisibleAttributes();
            List<DBCAttributeMetaData> meta = new ArrayList<>(attributes.size());
            boolean selectedColumnsOnly = proceedSelectedColumnsOnly(flags);
            for (DBDAttributeBinding attribute : attributes) {
                DBCAttributeMetaData metaAttribute = attribute.getMetaAttribute();
                if (!selectedColumnsOnly || options.getSelectedColumns().contains(metaAttribute.getName())) {
                    meta.add(metaAttribute);
                }
            }
            return new LocalResultSetMeta(meta);
        }

        @Override
        public String getResultSetName() throws DBCException {
            return "ClientResults";
        }

        @Override
        public void close() {
            // do nothing
        }
    }

}
