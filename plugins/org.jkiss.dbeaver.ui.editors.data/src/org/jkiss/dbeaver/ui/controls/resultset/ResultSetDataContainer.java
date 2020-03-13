/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
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
public class ResultSetDataContainer implements DBSDataContainer, DBPContextProvider, IAdaptable, DBDAttributeFilter {

    private static final Log log = Log.getLog(ResultSetDataContainer.class);

    private final IResultSetController controller;
    private final DBSDataContainer dataContainer;
    private final ResultSetModel model;
    private ResultSetDataContainerOptions options;
    private boolean filterAttributes;

    public ResultSetDataContainer(IResultSetController controller, ResultSetDataContainerOptions options) {
        this.controller = controller;
        this.dataContainer = controller.getDataContainer();
        this.model = controller.getModel();
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
    public DBCStatistics readData(DBCExecutionSource source, DBCSession session, DBDDataReceiver dataReceiver, DBDDataFilter dataFilter, long firstRow, long maxRows, long flags, int fetchSize) throws DBCException {
        filterAttributes = proceedSelectedColumnsOnly(flags);
        if (filterAttributes || proceedSelectedRowsOnly(flags)) {

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
            return dataContainer.readData(source, session, dataReceiver, dataFilter, firstRow, maxRows, flags, fetchSize);
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

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext() {
        return controller.getExecutionContext();
    }

    @Override
    public DBDAttributeBinding[] filterAttributeBindings(DBDAttributeBinding[] attributes) {
        DBDDataFilter dataFilter = model.getDataFilter();
        List<DBDAttributeBinding> filtered = new ArrayList<>();
        for (DBDAttributeBinding attr : attributes) {
            DBDAttributeConstraint ac = dataFilter.getConstraint(attr);
            if (ac != null && !ac.isVisible()) {
                continue;
            }
            if (!filterAttributes || options.getSelectedColumns().contains(attr.getName())) {
                filtered.add(attr);
            }
        }
        filtered.sort((o1, o2) -> {
            DBDAttributeConstraint c1 = dataFilter.getConstraint(o1, true);
            DBDAttributeConstraint c2 = dataFilter.getConstraint(o2, true);
            return c1 == null || c2 == null ? 0 : c1.getVisualPosition() - c2.getVisualPosition();
        });
        return filtered.toArray(new DBDAttributeBinding[0]);
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
                if (curRow.getVisualNumber() >= model.getRowCount() - 1) {
                    return false;
                }
                curRow = model.getRow(curRow.getVisualNumber() + 1);
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

        @NotNull
        @Override
        public DBCResultSetMetaData getMeta() throws DBCException {
            List<DBDAttributeBinding> attributes = model.getVisibleAttributes();
            List<DBCAttributeMetaData> meta = new ArrayList<>(attributes.size());
            for (DBDAttributeBinding attribute : attributes) {
                DBCAttributeMetaData metaAttribute = attribute.getMetaAttribute();
                if (metaAttribute != null) {
                    meta.add(metaAttribute);
                }
            }
            return new CustomResultSetMeta(meta);
        }

        @Override
        public String getResultSetName() throws DBCException {
            return "ClientResults";
        }

        @Override
        public Object getFeature(String name) {
            if (FEATURE_NAME_LOCAL.equals(name)) {
                return true;
            }
            return null;
        }

        @Override
        public void close() {
            // do nothing
        }

        private class CustomResultSetMeta extends LocalResultSetMeta {
            public CustomResultSetMeta(List<DBCAttributeMetaData> meta) {
                super(meta);
            }
        }

    }

}
