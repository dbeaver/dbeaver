/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IEvaluationService;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionSavepointInfo;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourcePropertyTester extends PropertyTester
{
    static final Log log = LogFactory.getLog(DataSourcePropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasource";
    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_TRANSACTIONAL = "transactional";
    public static final String PROP_TRANSACTION_ACTIVE = "transactionActive";

    public DataSourcePropertyTester() {
        super();
    }

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        if (!(receiver instanceof DBSDataSourceContainer)) {
            return false;
        }
        DBSDataSourceContainer dataSourceContainer = (DBSDataSourceContainer)receiver;
        if (PROP_CONNECTED.equals(property)) {
            return dataSourceContainer.isConnected() == Boolean.valueOf(String.valueOf(expectedValue));
        } else if (PROP_TRANSACTIONAL.equals(property)) {
            if (!dataSourceContainer.isConnected()) {
                return Boolean.FALSE.equals(expectedValue);
            }
            DBPDataSource dataSource = dataSourceContainer.getDataSource();
            if (dataSource == null || !dataSource.isConnected()) {
                return false;
            }
            DBCExecutionContext context = dataSource.openContext(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.META, "Check auto commit state");
            try {
                boolean transactional = !context.getTransactionManager().isAutoCommit();
                return Boolean.valueOf(transactional).equals(expectedValue);
            }
            catch (DBCException e) {
                log.error(e);
            }
            finally {
                context.close();
            }
        } else if (PROP_TRANSACTION_ACTIVE.equals(property)) {
            if (dataSourceContainer.isConnected()) {
                DBPDataSource dataSource = dataSourceContainer.getDataSource();
                QMMSessionInfo session = DBeaverCore.getInstance().getQueryManager().getMetaCollector().getSession(dataSource);
                QMMTransactionInfo transaction = session.getTransaction();
                if (transaction != null) {
                    QMMTransactionSavepointInfo savepoint = transaction.getCurrentSavepoint();
                    if (savepoint != null) {
                        QMMStatementExecuteInfo execute = savepoint.getLastExecute();
                        if (execute != null) {
                            return Boolean.TRUE.equals(expectedValue);
                        }
                    }
                }
            }
            return Boolean.FALSE.equals(expectedValue);
        }
        return false;
    }

    public static void firePropertyChange(String propName)
    {
        IEvaluationService service = (IEvaluationService) PlatformUI.getWorkbench().getService(IEvaluationService.class);
        service.requestEvaluation(NAMESPACE + "." + propName);
    }

}