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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class DataSourceTransactionModeContributor extends DataSourceMenuContributor {
    private static final Log log = Log.getLog(DataSourceTransactionModeContributor.class);

    @Override
    protected void fillContributionItems(final List<IContributionItem> menuItems) {
        IEditorPart activePart = UIUtils.getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        DBCExecutionContext executionContext = AbstractDataSourceHandler.getExecutionContextFromPart(activePart);

        DBPDataSource dataSource = null;
        if (executionContext != null) {
            dataSource = executionContext.getDataSource();
        }
        if (dataSource == null) {
            return;
        }
        final DBPDataSourceInfo dsInfo = dataSource.getInfo();

        DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
        if (txnManager != null) {
            boolean autoCommit = false;
            try {
                autoCommit = txnManager.isAutoCommit();
            } catch (DBCException e) {
                log.warn("Can't determine current auto-commit mode", e);
            }
            // Transactions
            DBPTransactionIsolation txnLevelCurrent = null;
            try {
                txnLevelCurrent = txnManager.getTransactionIsolation();
            } catch (DBCException ex) {
                log.warn("Can't determine current transaction isolation level", ex);
            }

            menuItems.add(ActionUtils.makeActionContribution(
                    new TransactionAutoCommitAction(executionContext, true, autoCommit, txnLevelCurrent),
                    true));
            menuItems.add(ActionUtils.makeActionContribution(
                    new TransactionAutoCommitAction(executionContext, false, !autoCommit, txnLevelCurrent),
                    true));
            ISmartTransactionManager smartTransactionManager = DBUtils.getAdapter(ISmartTransactionManager.class, activePart);
            menuItems.add(ActionUtils.makeActionContribution(
                    new SmartAutoCommitAction(dataSource, smartTransactionManager),
                    true));

            menuItems.add(new Separator());

            for (DBPTransactionIsolation txi : CommonUtils.safeCollection(dsInfo.getSupportedTransactionsIsolation())) {
                if (!txi.isEnabled()) {
                    continue;
                }
                menuItems.add(ActionUtils.makeActionContribution(
                        new TransactionIsolationAction(executionContext, txi, txi.equals(txnLevelCurrent)),
                        true));
            }
        }
    }

    private static class TransactionAutoCommitAction extends Action {
        private final DBCExecutionContext executionContext;
        private final boolean autoCommit;
        private final DBPTransactionIsolation isolation;

        TransactionAutoCommitAction(DBCExecutionContext executionContext, boolean autoCommit, boolean enabled, DBPTransactionIsolation isolation) {
            this.executionContext = executionContext;
            this.autoCommit = autoCommit;
            this.isolation = isolation;
            setChecked(enabled);
        }

        @Override
        public int getStyle() {
            return AS_RADIO_BUTTON;
        }

        @Override
        public String getText() {
            String isolationName = isolation == null ? "?" : isolation.getTitle();
            return autoCommit ? CoreMessages.action_menu_transaction_autocommit_description : NLS.bind(CoreMessages.action_menu_transaction_manualcommit_description, isolationName);
        }

        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
            if (txnManager != null) {
                new AbstractJob("Set auto-commit") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        monitor.beginTask("Change connection auto-commit to " + autoCommit, 1);
                        try {
                            monitor.subTask("Change context '" + executionContext.getContextName() + "' auto-commit state");
                            DBExecUtils.tryExecuteRecover(monitor, executionContext.getDataSource(), param -> {
                                try {
                                    txnManager.setAutoCommit(monitor, autoCommit);
                                } catch (DBCException e) {
                                    throw new InvocationTargetException(e);
                                }
                            });
                        } catch (Exception e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        } finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

    private static class SmartAutoCommitAction extends Action {
        private final ISmartTransactionManager smartTransactionManager;

         SmartAutoCommitAction(DBPDataSource dataSource, ISmartTransactionManager smartTransactionManager) {
            this.smartTransactionManager = smartTransactionManager;
            setEnabled(smartTransactionManager != null);

            setChecked(smartTransactionManager != null ?
                smartTransactionManager.isSmartAutoCommit() :
                dataSource.getContainer().getPreferenceStore().getBoolean(ModelPreferences.TRANSACTIONS_SMART_COMMIT));
        }

        @Override
        public int getStyle() {
            return AS_CHECK_BOX;
        }

        @Override
        public String getText() {
            return CoreMessages.action_menu_transaction_smart_auto_commit;
        }

        @Override
        public String getToolTipText() {
            return CoreMessages.action_menu_transaction_smart_auto_commit_tip;
        }

        @Override
        public boolean isChecked() {
             if (smartTransactionManager != null) {
                 return smartTransactionManager.isSmartAutoCommit();
             }
             return false;
        }

        @Override
        public void run() {
            smartTransactionManager.setSmartAutoCommit(!smartTransactionManager.isSmartAutoCommit());
        }
    }

    private static class TransactionIsolationAction extends Action {
        private final DBCExecutionContext executionContext;
        private final DBPTransactionIsolation level;

        TransactionIsolationAction(DBCExecutionContext executionContext, DBPTransactionIsolation level, boolean checked) {
            this.executionContext = executionContext;
            this.level = level;
            setChecked(checked);
        }

        @Override
        public int getStyle() {
            return AS_RADIO_BUTTON;
        }

        @Override
        public String getText() {
            return level.getTitle();
        }

        @Override
        public void run() {
            if (!isChecked()) {
                return;
            }
            DBCTransactionManager txnManager = DBUtils.getTransactionManager(executionContext);
            if (txnManager != null) {
                new AbstractJob("Set transaction isolation level") {
                    @Override
                    protected IStatus run(DBRProgressMonitor monitor) {
                        monitor.beginTask("Change transaction isolation level to " + level.getTitle(), 1);
                        try {
                            monitor.subTask("Change context '" + executionContext.getContextName() + "' transaction isolation level");
                            DBExecUtils.tryExecuteRecover(monitor, executionContext.getDataSource(), param -> {
                                try {
                                    txnManager.setTransactionIsolation(monitor, level);
                                } catch (DBCException e) {
                                    throw new InvocationTargetException(e);
                                }
                            });
                            executionContext.getDataSource().getContainer().setDefaultTransactionsIsolation(level);
                            executionContext.getDataSource().getContainer().persistConfiguration();
                        } catch (Exception e) {
                            return GeneralUtils.makeExceptionStatus(e);
                        } finally {
                            monitor.done();
                        }
                        return Status.OK_STATUS;
                    }
                }.schedule();
            }
        }
    }

}