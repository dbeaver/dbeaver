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

package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionSavepointInfo;

import java.util.Collections;
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
    
    public static QMExecutionHandler getDefaultHandler()
    {
        if (defaultHandler == null) {
            defaultHandler = application.getQueryManager().getDefaultHandler();
        }
        return defaultHandler;
    }

    public static void registerHandler(QMExecutionHandler handler)
    {
        application.getQueryManager().registerHandler(handler);
    }

    public static void unregisterHandler(QMExecutionHandler handler)
    {
        application.getQueryManager().unregisterHandler(handler);
    }

    public static void registerMetaListener(QMMetaListener metaListener)
    {
        application.getQueryManager().registerMetaListener(metaListener);
    }

    public static void unregisterMetaListener(QMMetaListener metaListener)
    {
        application.getQueryManager().unregisterMetaListener(metaListener);
    }

    public static List<QMMetaEvent> getPastMetaEvents()
    {
        if (application == null) {
            return Collections.emptyList();
        }
        QMController queryManager = application.getQueryManager();
        return queryManager == null ? Collections.<QMMetaEvent>emptyList() : queryManager.getPastMetaEvents();
    }

    public static boolean isTransactionActive(DBCExecutionContext executionContext) {
        if (executionContext == null || application == null) {
            return false;
        } else {
            QMMSessionInfo sessionInfo = getCurrentSession(executionContext);
            if (sessionInfo != null && sessionInfo.isTransactional()) {
                QMMTransactionInfo txnInfo = sessionInfo.getTransaction();
                if (txnInfo != null) {
                    QMMTransactionSavepointInfo sp = txnInfo.getCurrentSavepoint();
                    QMMStatementExecuteInfo execInfo = sp.getLastExecute();
                    for (QMMStatementExecuteInfo exec = execInfo; exec != null && exec.getSavepoint() == sp; exec = exec.getPrevious()) {
                        if (exec.isTransactional()) {
                            return true;
                        }
                    }
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
                        if (exec.isTransactional() && !exec.hasError() && purpose != DBCExecutionPurpose.META && purpose != DBCExecutionPurpose.UTIL) {
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
        return new QMTransactionState(execCount, updateCount, txnMode, txnStartTime);
    }

}
