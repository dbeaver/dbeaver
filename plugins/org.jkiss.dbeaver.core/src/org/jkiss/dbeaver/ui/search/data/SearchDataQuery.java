/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.search.data;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.ISearchResult;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.search.AbstractSearchResult;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
import java.util.*;

public class SearchDataQuery implements ISearchQuery {

    private static final Log log = Log.getLog(SearchDataQuery.class);

    private final SearchDataParams params;
    private SearchDataResult searchResult;

    private SearchDataQuery(SearchDataParams params)
    {
        this.params = params;
    }

    @Override
    public String getLabel()
    {
        return params.getSearchString();
    }

    @Override
    public boolean canRerun() {
        return true;
    }

    @Override
    public boolean canRunInBackground() {
        return true;
    }

    @Override
    public ISearchResult getSearchResult() {
        if (searchResult == null) {
            searchResult = new SearchDataResult(this);
        }
        return searchResult;
    }

    @Override
    public IStatus run(IProgressMonitor m) throws OperationCanceledException {
        try {
            String searchString = params.getSearchString();

            //monitor.subTask("Collect tables");
            Set<DBPDataSource> dataSources = new HashSet<>();
            for (DBSDataContainer searcher : params.sources) {
                dataSources.add(searcher.getDataSource());
            }

            // Search
            DBNModel dbnModel = DBWorkbench.getPlatform().getNavigatorModel();

            DBRProgressMonitor monitor = new DefaultProgressMonitor(m);

            int totalObjects = 0;

            monitor.beginTask(
                "Search \"" + searchString + "\" in " + params.sources.size() + " table(s) / " + dataSources.size() + " database(s)",
                params.sources.size());
            try {
                for (DBSDataContainer dataContainer : params.sources) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    if (searchDataInContainer(monitor, dbnModel, dataContainer)) {
                        totalObjects++;
                    }
                    monitor.worked(1);
                }
            } finally {
                monitor.done();
            }

            searchResult.fireChange(new AbstractSearchResult.DatabaseSearchFinishEvent(searchResult, totalObjects));

            return Status.OK_STATUS;
        } catch (Exception e) {
            return GeneralUtils.makeExceptionStatus(e);
        }
    }

    private boolean searchDataInContainer(DBRProgressMonitor monitor, DBNModel dbnModel, DBSDataContainer dataContainer) {
        if (!params.searchForeignObjects && dataContainer instanceof DBPForeignObject && ((DBPForeignObject) dataContainer).isForeignObject()) {
            return false;
        }

        String objectName = DBUtils.getObjectFullName(dataContainer, DBPEvaluationContext.DML);
        DBNDatabaseNode node = dbnModel.findNode(dataContainer);
        if (node == null) {
            log.warn("Can't find tree node for object \"" + objectName + "\"");
            return false;
        }
        monitor.subTask("Search in '" + objectName + "'");
        log.debug("Search in '" + objectName + "'");
        SearchTableMonitor searchMonitor = new SearchTableMonitor(monitor);
        try (DBCSession session = DBUtils.openUtilSession(searchMonitor, dataContainer, "Search rows in " + objectName)) {
            TestDataReceiver dataReceiver = new TestDataReceiver(searchMonitor);
            try {
                findRows(session, dataContainer, dataReceiver);
            } catch (DBCException e) {
                // Search failed in some container - just write an error in log.
                // We don't want to break whole search because of one single table.
                log.debug("Fulltext search failed in '" + dataContainer.getName() + "'", e);
            }

            if (dataReceiver.rowCount > 0) {
                SearchDataObject object = new SearchDataObject(node, dataReceiver.rowCount, dataReceiver.filter);
                searchResult.addObjects(Collections.singletonList(object));
                return true;
            }
            return false;
        }
    }

    private DBCStatistics findRows(
        @NotNull DBCSession session,
        @NotNull DBSDataContainer dataContainer,
        @NotNull TestDataReceiver dataReceiver) throws DBCException
    {
        DBSEntity entity;
        if (dataContainer instanceof DBSEntity) {
            entity = (DBSEntity) dataContainer;
        } else {
            log.warn("Data container " + dataContainer + " isn't entity");
            return null;
        }
        try {

            List<DBDAttributeConstraint> constraints = new ArrayList<>();
            for (DBSEntityAttribute attribute : CommonUtils.safeCollection(entity.getAttributes(session.getProgressMonitor()))) {
                if (params.fastSearch) {
                    if (DBUtils.findAttributeIndex(session.getProgressMonitor(), attribute) == null) {
                        continue;
                    }
                }
                if (DBUtils.isPseudoAttribute(attribute) || DBUtils.isHiddenObject(attribute)) {
                    continue;
                }
                DBCLogicalOperator[] supportedOperators = DBUtils.getAttributeOperators(attribute);
                DBCLogicalOperator operator;
                Object value;
                switch (attribute.getDataKind()) {
                    case BOOLEAN:
                        continue;
                    case NUMERIC:
                        if (!params.searchNumbers) {
                            continue;
                        }
                        if (!ArrayUtils.contains(supportedOperators, DBCLogicalOperator.EQUALS)) {
                            continue;
                        }
                        operator = DBCLogicalOperator.EQUALS;
                        try {
                            value = new Integer(params.searchString);
                        } catch (NumberFormatException e) {
                            try {
                                value = new Long(params.searchString);
                            } catch (NumberFormatException e1) {
                                try {
                                    value = new Double(params.searchString);
                                } catch (NumberFormatException e2) {
                                    try {
                                        value = new BigDecimal(params.searchString);
                                    } catch (Exception e3) {
                                        // Not a number
                                        continue;
                                    }
                                }
                            }
                        }
                        break;
                    case CONTENT:
                    case BINARY:
                        if (!params.searchLOBs) {
                            continue;
                        }
                    case STRING:
                        // Do not check value length. Some columns may be compressed/compacted/have special data type and thus have length < than value length.
//                        if (attribute.getMaxLength() > 0 && attribute.getMaxLength() < params.searchString.length()) {
//                            continue;
//                        }
                        if (ArrayUtils.contains(supportedOperators, DBCLogicalOperator.LIKE)) {
                            operator = DBCLogicalOperator.LIKE;
                            value = "%" + params.searchString + "%";
                        } else if (ArrayUtils.contains(supportedOperators, DBCLogicalOperator.EQUALS)) {
                            operator = DBCLogicalOperator.EQUALS;
                            value = params.searchString;
                        } else {
                            continue;
                        }
                        break;
                    default: {
                        // Try to convert string to attribute type
                        // On success search by exact match
                        if (!ArrayUtils.contains(supportedOperators, DBCLogicalOperator.EQUALS)) {
                            continue;
                        }
                        String typeName = attribute.getTypeName();
                        if (typeName.equals(DBConstants.TYPE_NAME_UUID) || typeName.equals(DBConstants.TYPE_NAME_UUID2)) {
                            try {
                                UUID uuid = UUID.fromString(params.searchString);
                                operator = DBCLogicalOperator.EQUALS;
                                value = uuid.toString();
                            } catch (Exception e) {
                                // No a UUID
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                DBDAttributeConstraint constraint = new DBDAttributeConstraint(attribute, constraints.size());
                constraint.setOperator(operator);
                constraint.setValue(value);
                constraint.setVisible(true);
                constraints.add(constraint);
            }
            if (constraints.isEmpty()) {
                return null;
            }
            dataReceiver.filter = new DBDDataFilter(constraints);
            dataReceiver.filter.setAnyConstraint(true);
            DBCExecutionSource searchSource = new AbstractExecutionSource(dataContainer, session.getExecutionContext(), this);
            return dataContainer.readData(searchSource, session, dataReceiver, dataReceiver.filter, -1, -1, 0);
        } catch (DBException e) {
            throw new DBCException("Error finding rows", e);
        }
    }

    public static SearchDataQuery createQuery(SearchDataParams params)
        throws DBException
    {
        return new SearchDataQuery(params);
    }

    private class SearchTableMonitor extends VoidProgressMonitor {

        private DBRProgressMonitor baseMonitor;
        private volatile boolean canceled;

        private SearchTableMonitor(DBRProgressMonitor monitor) {
            this.baseMonitor = monitor;
        }

        @Override
        public boolean isCanceled() {
            return canceled || baseMonitor.isCanceled();
        }
    }

    private class TestDataReceiver implements DBDDataReceiver {

        private SearchTableMonitor searchMonitor;
        private int rowCount = 0;
        private DBDDataFilter filter;

        public TestDataReceiver(SearchTableMonitor searchMonitor) {
            this.searchMonitor = searchMonitor;
        }

        @Override
        public void fetchStart(DBCSession session, DBCResultSet resultSet, long offset, long maxRows) throws DBCException {

        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
            rowCount++;
            if (rowCount >= params.maxResults) {
                searchMonitor.canceled = true;
            }
        }

        @Override
        public void fetchEnd(DBCSession session, DBCResultSet resultSet) throws DBCException {

        }

        @Override
        public void close() {

        }
    }

}
