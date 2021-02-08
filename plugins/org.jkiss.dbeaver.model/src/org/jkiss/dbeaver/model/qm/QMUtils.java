/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionSavepointInfo;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Query Manager utils
 */
public class QMUtils {

    private static DBPPlatform application;
    private static QMExecutionHandler defaultHandler;

    public static void initApplication(DBPPlatform application) {
        QMUtils.application = application;
    }

    public static QMExecutionHandler getDefaultHandler() {
        if (defaultHandler == null) {
            defaultHandler = application.getQueryManager().getDefaultHandler();
        }
        return defaultHandler;
    }

    public static void registerHandler(QMExecutionHandler handler) {
        application.getQueryManager().registerHandler(handler);
    }

    public static void unregisterHandler(QMExecutionHandler handler) {
        application.getQueryManager().unregisterHandler(handler);
    }

    public static void registerMetaListener(QMMetaListener metaListener) {
        application.getQueryManager().registerMetaListener(metaListener);
    }

    public static void unregisterMetaListener(QMMetaListener metaListener) {
        application.getQueryManager().unregisterMetaListener(metaListener);
    }

    @Nullable
    public static QMEventBrowser getEventBrowser(boolean currentSessionOnly) {
        if (application == null) {
            return null;
        }
        return application.getQueryManager().getEventBrowser(currentSessionOnly);
    }

    public static boolean isTransactionActive(DBCExecutionContext executionContext) {
        return isTransactionActive(executionContext, true);
    }

    public static boolean isTransactionActive(DBCExecutionContext executionContext, boolean checkQueries) {
        if (executionContext == null || application == null) {
            return false;
        } else {
            QMMSessionInfo sessionInfo = getCurrentSession(executionContext);
            if (sessionInfo != null && sessionInfo.isTransactional()) {
                QMMTransactionInfo txnInfo = sessionInfo.getTransaction();
                if (txnInfo != null) {
                    QMMTransactionSavepointInfo sp = txnInfo.getCurrentSavepoint();
                    if (sp != null) {
                        if (checkQueries) {
                            // If transaction was enabled all statements are transactional
                            for (QMMStatementExecuteInfo ei = sp.getLastExecute(); ei != null; ei = ei.getPrevious()) {
                                if (ei.isTransactional()) {
                                    return true;
                                }
                            }
                        } else {
                            return sp.getLastExecute() != null;
                        }
                    }
//                    for (QMMStatementExecuteInfo exec = execInfo; exec != null && exec.getSavepoint() == sp; exec = exec.getPrevious()) {
//                        if (exec.isTransactional()) {
//                            return true;
//                        }
//                    }
                }
            }
        }
        return false;
    }

    public static QMMSessionInfo getCurrentSession(DBCExecutionContext executionContext) {
        return application.getQueryManager().getMetaCollector().getSessionInfo(executionContext);
    }

    public static QMMTransactionSavepointInfo getCurrentTransaction(DBCExecutionContext executionContext) {
        QMMSessionInfo sessionInfo = getCurrentSession(executionContext);
        if (sessionInfo != null && !sessionInfo.isClosed() && sessionInfo.isTransactional()) {
            QMMTransactionInfo txnInfo = sessionInfo.getTransaction();
            if (txnInfo != null) {
                return txnInfo.getCurrentSavepoint();
            }
        }
        return null;
    }

    @NotNull
    public static QMTransactionState getTransactionState(DBCExecutionContext executionContext) {
        int execCount = 0, updateCount = 0;
        final boolean txnMode;
        long txnStartTime = 0;
        if (executionContext == null || application == null) {
            txnMode = false;
        } else {
            QMMSessionInfo sessionInfo = getCurrentSession(executionContext);
            if (sessionInfo == null || sessionInfo.isClosed()) {
                txnMode = false;
            } else if (sessionInfo.isTransactional()) {
                QMMTransactionInfo txnInfo = sessionInfo.getTransaction();
                if (txnInfo != null) {
                    txnMode = true;
                    QMMTransactionSavepointInfo sp = txnInfo.getCurrentSavepoint();
                    QMMStatementExecuteInfo execInfo = sp.getLastExecute();
                    for (QMMStatementExecuteInfo exec = execInfo; exec != null && exec.getSavepoint() == sp; exec = exec.getPrevious()) {
                        execCount++;
                        DBCExecutionPurpose purpose = exec.getStatement().getPurpose();
                        if (!exec.hasError() && purpose != DBCExecutionPurpose.META && purpose != DBCExecutionPurpose.UTIL) {
                            txnStartTime = exec.getOpenTime();
                            updateCount++;
                        }
                    }
                } else {
                    // No active transaction?
                    txnMode = false;
                }
            } else {
                txnMode = false;
            }
        }
        if (txnStartTime <= 0) {
            txnStartTime = System.currentTimeMillis();
        }
        return new QMTransactionState(execCount, updateCount, txnMode, txnStartTime);
    }

    public static QMEventCriteria createDefaultCriteria(DBPPreferenceStore store) {
        QMEventCriteria criteria = new QMEventCriteria();

        Collection<QMObjectType> objectTypes = QMObjectType.fromString(store.getString(QMConstants.PROP_OBJECT_TYPES));
        criteria.setObjectTypes(objectTypes.toArray(new QMObjectType[0]));
        List<DBCExecutionPurpose> queryTypes = new ArrayList<>();
        for (String queryType : CommonUtils.splitString(store.getString(QMConstants.PROP_QUERY_TYPES), ',')) {
            try {
                queryTypes.add(DBCExecutionPurpose.valueOf(queryType));
            } catch (IllegalArgumentException e) {
                // ignore
            }
        }
        criteria.setQueryTypes(queryTypes.toArray(new DBCExecutionPurpose[0]));
        return criteria;
    }

    public static class ListCursorImpl implements QMEventCursor {

        private final List<QMMetaEvent> events;
        private int position;

        public ListCursorImpl(List<QMMetaEvent> events) {
            this.events = events;
            this.position = 0;
        }

        @Override
        public long getTotalSize() {
            return events.size();
        }

        @Override
        public void scroll(int position, DBRProgressMonitor monitor) throws DBException {
            if (position < 0 || position >= events.size()) {
                throw new DBException("Position is out of range (" + getTotalSize() + ")");
            }
        }

        @Override
        public boolean hasNextEvent(DBRProgressMonitor monitor) throws DBException {
            return position < events.size();
        }

        @Override
        public QMMetaEvent nextEvent(DBRProgressMonitor monitor) throws DBException {
            QMMetaEvent event = events.get(position);
            position++;
            return event;
        }

        @Override
        public void close() {

        }

    }

    public static class EmptyCursorImpl implements QMEventCursor {

        @Override
        public long getTotalSize() {
            return 0;
        }

        @Override
        public void scroll(int position, DBRProgressMonitor monitor) throws DBException {
            throw new DBException("Empty cursor");
        }

        @Override
        public boolean hasNextEvent(DBRProgressMonitor monitor) throws DBException {
            return false;
        }

        @Override
        public QMMetaEvent nextEvent(DBRProgressMonitor monitor) throws DBException {
            throw new DBException("Empty cursor");
        }

        @Override
        public void close() {

        }

    }

}
