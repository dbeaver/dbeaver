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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.INewWizard;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.ModelPreferences.SeparateConnectionBehavior;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.*;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.ConnectionTestJob;
import org.jkiss.dbeaver.ui.ConnectionFeatures;
import org.jkiss.dbeaver.ui.IDataSourceConnectionTester;
import org.jkiss.dbeaver.ui.IDialogPageProvider;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizard;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.dialogs.IConnectionWizard;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends ActiveWizard implements IConnectionWizard, INewWizard {

    static final String PROP_CONNECTION_TYPE = "connection-type";

    // protected final IProject project;
    private final Map<DriverDescriptor, DataSourceDescriptor> infoMap = new HashMap<>();
    private final List<IPropertyChangeListener> propertyListeners = new ArrayList<>();
    private DBPDriverSubstitutionDescriptor driverSubstitution;

    protected ConnectionWizard() {
        setNeedsProgressMonitor(true);
        //setDefaultPageImageDescriptor(DBeaverActivator.getImageDescriptor("icons/driver-logo.png"));
    }

    @Override
    public String getWindowTitle() {
        if (isNew()) {
            return CoreMessages.dialog_new_connection_wizard_title;
        } else {
            DataSourceDescriptor activeDataSource = getActiveDataSource();
            return NLS.bind( CoreMessages.dialog_connection_edit_title, activeDataSource.getName());
        }
    }

    @Override
    public Image getDefaultPageImage() {
        return super.getDefaultPageImage();
//        DBPDriver selectedDriver = getSelectedDriver();
//        return DBeaverIcons.getImage(selectedDriver == null ? DBIcon.DATABASE_DEFAULT : selectedDriver.getIcon());
    }

    @Override
    public void dispose() {
        // Dispose all temp data sources
        for (DataSourceDescriptor dataSource : infoMap.values()) {
            dataSource.dispose();
        }
        super.dispose();
    }

    @Nullable
    abstract public DBPDataSourceRegistry getDataSourceRegistry();

    abstract DBPDriver getSelectedDriver();

    abstract DBPProject getSelectedProject();

    abstract DBNBrowseSettings getSelectedNavigatorSettings();

    public abstract ConnectionPageSettings getPageSettings();

    protected abstract void saveSettings(DataSourceDescriptor dataSource);

    @NotNull
    public DataSourceDescriptor getActiveDataSource() {
        DriverDescriptor driver = (DriverDescriptor) getSelectedDriver();
        DataSourceDescriptor info = infoMap.get(driver);
        DBPDataSourceRegistry registry = getDataSourceRegistry();
        if (registry == null) {
            throw new IllegalStateException("No active project");
        }
        if (info == null && driver != null) {
            DBPConnectionConfiguration connectionInfo = getDefaultConnectionConfiguration();
            info = registry.createDataSource(
                DataSourceDescriptor.generateNewId(driver),
                driver,
                connectionInfo
            );
            DBPNativeClientLocation defaultClientLocation = driver.getDefaultClientLocation();
            if (defaultClientLocation != null) {
                info.getConnectionConfiguration().setClientHomeId(defaultClientLocation.getName());
            }
            info.setSavePassword(true);
            infoMap.put(driver, info);
        }
        return info;
    }

    @Nullable
    public abstract DataSourceDescriptor getOriginalDataSource();

    @Nullable
    @Override
    public DBPDriverSubstitutionDescriptor getDriverSubstitution() {
        return driverSubstitution;
    }

    @NotNull
    protected abstract PersistResult persistDataSource();

    public void setDriverSubstitution(@Nullable DBPDriverSubstitutionDescriptor driverSubstitution) {
        this.driverSubstitution = driverSubstitution;
        getActiveDataSource().setDriverSubstitution(driverSubstitution);
    }

    public void testConnection() {
        DataSourceDescriptor activeDataSource = getActiveDataSource();
        DataSourceDescriptor targetDataSource;

        if (canUseTemporaryDataSource(activeDataSource)) {
            targetDataSource = activeDataSource.getRegistry().createDataSource(activeDataSource);
            // Generate new ID to avoid session conflicts in QM
            targetDataSource.setId(DataSourceDescriptor.generateNewId(activeDataSource.getDriver()));
            targetDataSource.setTemporary(true);
            targetDataSource.getPreferenceStore().setValue(
                ModelPreferences.META_SEPARATE_CONNECTION,
                SeparateConnectionBehavior.NEVER.name()
            );
        } else {
            int decision = ConfirmationDialog.confirmAction(
                getShell(),
                ConfirmationDialog.WARNING,
                DBeaverPreferences.CONFIRM_TEST_CONNECTION_PERSIST,
                ConfirmationDialog.CONFIRM
            );
            if (decision != IDialogConstants.OK_ID) {
                return;
            }
            targetDataSource = activeDataSource;
        }

        saveSettings(targetDataSource);

        if (activeDataSource == targetDataSource) {
            persistDataSource();
        }

        if (targetDataSource.isSharedCredentials()) {
            if (!targetDataSource.getProject().getWorkspace().hasRealmPermission(RMConstants.PERMISSION_PROJECT_ADMIN)) {
                UIUtils.showMessageBox(getShell(), "Credentials edit restricted",
                    "Shared credentials edit is available for administrators only.",
                    SWT.ICON_ERROR);
            } else {
                UIUtils.showMessageBox(getShell(), "Use credentials manager",
                    "Direct connection test is not available for shared connections.\nGo to shared credentials manager dialog.",
                    SWT.ICON_WARNING);
            }
            return;
        }

        ConnectionFeatures.CONNECTION_TEST.use(Map.of("driver", targetDataSource.getDriver().getPreconfiguredId()));

        try {
            final ConnectionTestJob op = new ConnectionTestJob(targetDataSource, session -> {
                for (IWizardPage page : getPages()) {
                    testInPage(session, page);
                }
            });

            try {
                getRunnableContext().run(true, true, monitor -> {
                    // Wait for job to finish
                    op.setOwnerMonitor(monitor);
                    op.schedule();
                    while (op.getState() == Job.WAITING || op.getState() == Job.RUNNING) {
                        if (monitor.isCanceled()) {
                            op.cancel();
                            throw new InterruptedException();
                        }
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                    if (op.getConnectError() != null) {
                        throw new InvocationTargetException(op.getConnectError());
                    }
                    if (op.getConnectStatus() == Status.CANCEL_STATUS) {
                        throw new InterruptedException("cancel");
                    }
                });

                new ConnectionTestDialog(
                    getShell(),
                    targetDataSource,
                    op.getServerVersion(),
                    op.getClientVersion(),
                    op.getConnectTime()).open();

            } catch (InterruptedException ex) {
                if (!"cancel".equals(ex.getMessage())) {
                    DBWorkbench.getPlatformUI().showError(CoreMessages.dialog_connection_wizard_start_dialog_interrupted_title,
                        CoreMessages.dialog_connection_wizard_start_dialog_interrupted_message);
                }
            } catch (InvocationTargetException ex) {
                String msg = GeneralUtils.getExceptionMessage(ex);
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    msg,
                    GeneralUtils.makeExceptionStatus(ex.getTargetException()));
            } catch (Throwable ex) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    null,
                    GeneralUtils.makeExceptionStatus(ex));
            }
        } finally {
            if (activeDataSource != targetDataSource) {
                targetDataSource.dispose();
            }
        }
    }

    @Override
    public boolean isNew() {
        return false;
    }

    private void testInPage(DBCSession session, IDialogPage page) {
        if (page instanceof IDataSourceConnectionTester) {
            if (page.getControl() != null && !page.getControl().isDisposed()) {
                ((IDataSourceConnectionTester) page).testConnection(session);
            }
        }
        if (page instanceof IDialogPageProvider && isPageActive(page)) {
            for (IDialogPage subPage : ArrayUtils.safeArray(((IDialogPageProvider) page).getDialogPages(false, false))) {
                testInPage(session, subPage);
            }
        }
    }

    @Override
    public void addPropertyChangeListener(@NotNull IPropertyChangeListener listener) {
        propertyListeners.add(listener);
    }

    @Override
    public void firePropertyChangeEvent(@NotNull String property, @Nullable Object oldValue, @Nullable Object newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        for (IPropertyChangeListener listener : propertyListeners) {
            listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
        }
    }

    public boolean openSettingsPage(String pageId) {
        final IWizardPage page = getPage(pageId);
        if (page != null) {
            getContainer().showPage(page);
            return true;
        }
        return false;
    }

    @NotNull
    protected DBPConnectionConfiguration getDefaultConnectionConfiguration() {
        DBPConnectionType type = DBPConnectionType.getDefaultConnectionType();

        DBPConnectionConfiguration config = new DBPConnectionConfiguration();
        config.setConnectionType(type);
        config.setCloseIdleConnection(type.isAutoCloseConnections());

        return config;
    }

    private static boolean canUseTemporaryDataSource(@NotNull DataSourceDescriptor descriptor) {
        for (DBWHandlerConfiguration handler : descriptor.getConnectionConfiguration().getHandlers()) {
            if (handler.isEnabled() && handler.getHandlerDescriptor().isDistributed()) {
                return false;
            }
        }
        return true;
    }

    protected enum PersistResult {
        UNCHANGED,
        CHANGED,
        ERROR
    }
}