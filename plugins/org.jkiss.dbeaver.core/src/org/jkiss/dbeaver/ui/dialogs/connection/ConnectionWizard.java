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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.INewWizard;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.navigator.DBNBrowseSettings;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.driver.DriverDescriptor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.jobs.ConnectionTestJob;
import org.jkiss.dbeaver.ui.ICompositeDialogPage;
import org.jkiss.dbeaver.ui.IDataSourceConnectionTester;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizard;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract connection wizard
 */

public abstract class ConnectionWizard extends ActiveWizard implements INewWizard {

    static final String PROP_CONNECTION_TYPE = "connection-type";

    // protected final IProject project;
    private final Map<DriverDescriptor, DataSourceDescriptor> infoMap = new HashMap<>();
    private final List<IPropertyChangeListener> propertyListeners = new ArrayList<>();

    protected ConnectionWizard() {
        setNeedsProgressMonitor(true);
        setDefaultPageImageDescriptor(DBeaverActivator.getImageDescriptor("icons/driver-logo.png"));
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
        if (info == null) {
            DBPConnectionConfiguration connectionInfo = new DBPConnectionConfiguration();
            info = new DataSourceDescriptor(
                getDataSourceRegistry(),
                DataSourceDescriptor.generateNewId(getSelectedDriver()),
                driver,
                connectionInfo);
            DBPNativeClientLocation defaultClientLocation = driver.getDefaultClientLocation();
            if (defaultClientLocation != null) {
                info.getConnectionConfiguration().setClientHomeId(defaultClientLocation.getName());
            }
            info.setSavePassword(true);
            infoMap.put(driver, info);
        }
        return info;
    }

    public DataSourceDescriptor getOriginalDataSource() {
        return null;
    }


    public void testConnection() {
        DataSourceDescriptor dataSource = getPageSettings().getActiveDataSource();
        DataSourceDescriptor testDataSource = new DataSourceDescriptor(dataSource);

        saveSettings(testDataSource);
        testDataSource.setTemporary(true);

        // Generate new ID to avoid session conflicts in QM
        testDataSource.setId(DataSourceDescriptor.generateNewId(dataSource.getDriver()));
        testDataSource.getPreferenceStore().setValue(ModelPreferences.META_SEPARATE_CONNECTION, false);

        try {

            final ConnectionTestJob op = new ConnectionTestJob(testDataSource, session -> {
                for (IWizardPage page : getPages()) {
                    testInPage(session, page);
                }
            });

            try {
                getContainer().run(true, true, monitor -> {
                    // Wait for job to finish
                    op.setOwnerMonitor(RuntimeUtils.makeMonitor(monitor));
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
                    op.getServerVersion(),
                    op.getClientVersion(),
                    op.getConnectTime()).open();

            } catch (InterruptedException ex) {
                if (!"cancel".equals(ex.getMessage())) {
                    DBWorkbench.getPlatformUI().showError(CoreMessages.dialog_connection_wizard_start_dialog_interrupted_title,
                        CoreMessages.dialog_connection_wizard_start_dialog_interrupted_message);
                }
            } catch (InvocationTargetException ex) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    null,
                    GeneralUtils.makeExceptionStatus(ex.getTargetException()));
            } catch (Throwable ex) {
                DBWorkbench.getPlatformUI().showError(
                    CoreMessages.dialog_connection_wizard_start_dialog_error_title,
                    null,
                    GeneralUtils.makeExceptionStatus(ex));
            }
        } finally {
            testDataSource.dispose();
        }
    }

    public boolean isNew() {
        return false;
    }

    private void testInPage(DBCSession session, IDialogPage page) {
        if (page instanceof IDataSourceConnectionTester) {
            if (page.getControl() != null && !page.getControl().isDisposed()) {
                ((IDataSourceConnectionTester) page).testConnection(session);
            }
        }
        if (page instanceof ICompositeDialogPage && isPageActive(page)) {
            for (IDialogPage subPage : ArrayUtils.safeArray(((ICompositeDialogPage) page).getSubPages(false, false))) {
                testInPage(session, subPage);
            }
        }
    }

    public void addPropertyChangeListener(IPropertyChangeListener listener) {
        propertyListeners.add(listener);
    }

    public void firePropertyChangeEvent(String property, Object oldValue, Object newValue) {
        for (IPropertyChangeListener listener : propertyListeners) {
            listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
        }
    }

    public boolean openSettingsPage(String pageId) {
        final IWizardPage page = getPage(pageId);
        if (page != null) {
            getContainer().showPage(page);
        }
        return false;
    }

}