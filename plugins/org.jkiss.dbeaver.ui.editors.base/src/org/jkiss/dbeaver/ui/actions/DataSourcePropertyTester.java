/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.runtime.IPluginService;
import org.jkiss.dbeaver.runtime.qm.DefaultExecutionHandler;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.ISmartTransactionManager;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.IStatefulEditor;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * DatabaseEditorPropertyTester
 */
public class DataSourcePropertyTester extends PropertyTester {

    static protected final Log log = Log.getLog(DataSourcePropertyTester.class);

    public static final String NAMESPACE = "org.jkiss.dbeaver.core.datasource";
    public static final String PROP_CONNECTED = "connected";
    public static final String PROP_CONNECTING = "connecting";
    public static final String PROP_TRANSACTIONAL = "transactional";
    public static final String PROP_SYNCHRONIZABLE = "synchronizable";
    public static final String PROP_SUPPORTS_TRANSACTIONS = "supportsTransactions";
    public static final String PROP_TRANSACTION_ACTIVE = "transactionActive";
    public static final String PROP_EDITABLE = "editable";
    public static final String PROP_PROJECT_RESOURCE_EDITABLE = "projectResourceEditable";
    public static final String PROP_PROJECT_RESOURCE_VIEWABLE = "projectResourceViewable";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        try {
            // Get root datasource node (we don't want to
            while (receiver instanceof DBNDatabaseNode node && !(receiver instanceof DBNDataSource)) {
                receiver = node.getParentNode();
            }
            if (!(receiver instanceof DBPContextProvider contextProvider)) {
                return false;
            }
            @Nullable
            DBCExecutionContext context = contextProvider.getExecutionContext();

            switch (property) {
                case PROP_CONNECTED:
                    boolean isConnected;
                    if (context != null) {
                        isConnected = context.getDataSource().getContainer().isConnected();
                    } else if (receiver instanceof DBPDataSourceContainerProvider containerProvider) {
                        DBPDataSourceContainer container = containerProvider.getDataSourceContainer();
                        isConnected = container != null && container.isConnected();
                    } else {
                        isConnected = false;
                    }
                    boolean checkConnected = Boolean.TRUE.equals(expectedValue);
                    return checkConnected == isConnected;
                case PROP_CONNECTING: {
                    if (receiver instanceof DBPDataSourceContainerProvider containerProvider) {
                        DBPDataSourceContainer container = containerProvider.getDataSourceContainer();
                        return container != null && container.isConnecting();
                    }
                    return false;
                }
                case PROP_TRANSACTIONAL: {
                    if (context == null) {
                        return false;
                    }
                    if (!context.isConnected()) {
                        return Boolean.FALSE.equals(expectedValue);
                    }
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                    try {
                        return txnManager != null && Boolean.valueOf(!txnManager.isAutoCommit()).equals(expectedValue);
                    } catch (DBCException e) {
                        log.debug("Error checking auto-commit state", e);
                        return false;
                    }
                }
                case PROP_SYNCHRONIZABLE:
                    if (context == null || !context.isConnected()) {
                        return false;
                    } else {
                        final var container = context.getDataSource().getContainer();
                        final var provider = container.getDriver().getDataSourceProvider();
                        final var providerSynchronizable = GeneralUtils.adapt(provider, DBPDataSourceProviderSynchronizable.class);
                        return providerSynchronizable != null && providerSynchronizable.isSynchronizationEnabled(container);
                    }
                case PROP_SUPPORTS_TRANSACTIONS: {
                    if (context == null || !context.isConnected()) {
                        return false;
                    }
                    DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                    return txnManager != null && txnManager.isSupportsTransactions();
                }
                case PROP_TRANSACTION_ACTIVE: {
                    if (context != null && context.isConnected()) {
                        if (receiver instanceof IPageChangeProvider pcp) {
                            Object selectedPage = pcp.getSelectedPage();
                            if (!(selectedPage instanceof ISmartTransactionManager)) {
                                return Boolean.FALSE.equals(expectedValue);
                            }
                        }
                        DBCTransactionManager txnManager = DBUtils.getTransactionManager(context);
                        return txnManager != null && !txnManager.isAutoCommit();
//                        boolean active = QMUtils.isTransactionActive(context);
//                        return Boolean.valueOf(active).equals(expectedValue);
                    }
                    return Boolean.FALSE.equals(expectedValue);
                }
                case PROP_EDITABLE: {
                    DBPProject resourceProject = getProject(receiver);
                    return resourceProject == null || resourceProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT);
                }
                case PROP_PROJECT_RESOURCE_EDITABLE: {
                    DBPProject resourceProject = getProject(receiver);
                    return resourceProject == null || resourceProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT);
                }
                case PROP_PROJECT_RESOURCE_VIEWABLE: {
                    DBPProject resourceProject = getProject(receiver);
                    return resourceProject == null || resourceProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW);
                }
            }
            return false;
        } catch (Exception e) {
            log.debug("Error testing property " + property + ": " + e.getMessage());
            return false;
        }
    }

    private static @Nullable DBPProject getProject(Object receiver) {
        return receiver instanceof IEditorPart editorPart
            ? EditorUtils.getFileProject(editorPart.getEditorInput())
            : null;
    }

    public static void firePropertyChange(String propName)
    {
        ActionUtils.evaluatePropertyState(NAMESPACE + "." + propName);
    }

    public static class QMService implements IPluginService {

        private QMEventsHandler qmHandler;

        @Override
        public void activateService() {
            qmHandler = new QMEventsHandler();
            QMUtils.registerHandler(qmHandler);
        }

        @Override
        public void deactivateService() {
            QMUtils.unregisterHandler(qmHandler);
        }
    }

    // QM events handler
    private static class QMEventsHandler extends DefaultExecutionHandler {
        @NotNull
        @Override
        public String getHandlerName() {
            return DataSourcePropertyTester.class.getName();
        }

        @Override
        public synchronized void handleTransactionAutocommit(@NotNull DBCExecutionContext context, boolean autoCommit) {
            updateUI(() -> {
                // Fire transactional mode change
                DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTIONAL);
                DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
                ActionUtils.fireCommandRefresh(ConnectionCommands.CMD_TOGGLE_AUTOCOMMIT);
            });
        }

        @Override
        public synchronized void handleTransactionCommit(@NotNull DBCExecutionContext context) {
            updateUI(() -> {
                DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
                updateEditorsDirtyFlag();
            });
        }

        @Override
        public synchronized void handleTransactionRollback(@NotNull DBCExecutionContext context, DBCSavepoint savepoint) {
            updateUI(() -> {
                DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE);
                updateEditorsDirtyFlag();
            });
        }

        @Override
        public synchronized void handleStatementExecuteBegin(@NotNull DBCStatement statement) {
            updateUI(() -> DataSourcePropertyTester.firePropertyChange(DataSourcePropertyTester.PROP_TRANSACTION_ACTIVE));
        }

        private void updateUI(Runnable runnable) {
            UIUtils.asyncExec(runnable);
        }
    }

    /**
     * This is a hack.
     * Editors should listen txn commit/rollback and update their dirty flag (active transaction makes SQL editor dirty).
     * Making each editor QM listener is too expensive.
     */
    private static void updateEditorsDirtyFlag() {
        IWorkbenchWindow workbenchWindow = UIUtils.findActiveWorkbenchWindow();
        if (workbenchWindow == null) {
            return;
        }
        IEditorReference[] editors = workbenchWindow.getActivePage().getEditorReferences();
        for (IEditorReference ref : editors) {
            final IEditorPart editor = ref.getEditor(false);
            if (editor instanceof IStatefulEditor) {
                UIUtils.asyncExec(((IStatefulEditor) editor)::updateDirtyFlag);
            }
        }
    }

}