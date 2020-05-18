/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDataSourceDialog;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * TargetPrefPage
 */
public abstract class TargetPrefPage extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {
    static final Log log = Log.getLog(TargetPrefPage.class);

    private IAdaptable element;
    private DBPDataSourceContainer dataSourceContainer;
    private DBNDataSource containerNode;

    private Button dataSourceSettingsButton;
    private Control configurationBlockControl;
    private Link changeSettingsTargetLink;
    private ControlEnableState blockEnableState;

    protected TargetPrefPage() {
    }

    public final boolean isDataSourcePreferencePage() {
        return dataSourceContainer != null;
    }

    protected abstract boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dsContainer);

    protected abstract boolean supportsDataSourceSpecificOptions();

    protected void createPreferenceHeader(Composite composite) {
    }

    protected abstract Control createPreferenceContent(Composite composite);

    protected abstract void loadPreferences(DBPPreferenceStore store);

    protected abstract void savePreferences(DBPPreferenceStore store);

    protected abstract void clearPreferences(DBPPreferenceStore store);

    protected abstract String getPropertyPageID();

    public DBPDataSourceContainer getDataSourceContainer() {
        return dataSourceContainer;
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    @Override
    public IAdaptable getElement() {
        return element;
    }

    @Override
    public void setElement(IAdaptable element) {
        this.element = element;
        if (this.element == null) {
            return;
        }
        dataSourceContainer = element instanceof DBPDataSourceContainer ? (DBPDataSourceContainer)element : null;

        containerNode = element.getAdapter(DBNDataSource.class);
        if (containerNode == null) {
            final DBPDataSourceContainer dsContainer = element.getAdapter(DBPDataSourceContainer.class);
            if (dsContainer != null) {
                containerNode = (DBNDataSource) DBWorkbench.getPlatform().getNavigatorModel().findNode(dsContainer);
            } else {
                IDatabaseEditorInput dbInput = element.getAdapter(IDatabaseEditorInput.class);
                if (dbInput != null) {
                    DBNNode dbNode = dbInput.getNavigatorNode();
                    if (dbNode instanceof DBNDataSource) {
                        containerNode = (DBNDataSource) dbNode;
                    }
                } else if (element instanceof DBPContextProvider) {
                    DBCExecutionContext context = ((DBPContextProvider) element).getExecutionContext();
                    if (context != null) {
                        containerNode = (DBNDataSource) DBWorkbench.getPlatform().getNavigatorModel().findNode(context.getDataSource().getContainer());
                    }
                } else if (element instanceof DBPDataSourceContainer) {
                    containerNode = (DBNDataSource) DBWorkbench.getPlatform().getNavigatorModel().findNode((DBPDataSourceContainer) element);
                }
            }
        }
        if (dataSourceContainer == null && containerNode != null) {
            dataSourceContainer = containerNode.getDataSourceContainer();
        }
    }


    public void applyData(Object data) {
        if (containerNode == null && data instanceof IAdaptable) {
            setElement((IAdaptable) data);
        }
    }

    @Override
    protected Label createDescriptionLabel(Composite parent) {
        if (isDataSourcePreferencePage()) {
            Composite composite = UIUtils.createPlaceholder(parent, 2);
            composite.setFont(parent.getFont());
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            dataSourceSettingsButton = new Button(composite, SWT.CHECK);
            dataSourceSettingsButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    boolean enabled = dataSourceSettingsButton.getSelection();
                    enableDataSourceSpecificSettings(enabled);
                }
            });
            String dataSourceName = dataSourceContainer.getName();
            dataSourceSettingsButton.setText(NLS.bind(UINavigatorMessages.pref_page_target_button_use_datasource_settings, dataSourceName));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            dataSourceSettingsButton.setLayoutData(gd);

            changeSettingsTargetLink = createLink(composite, UINavigatorMessages.pref_page_target_link_show_global_settings);
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        } else if (supportsDataSourceSpecificOptions()) {
            changeSettingsTargetLink = createLink(
                parent,
                UINavigatorMessages.pref_page_target_link_show_datasource_settings);
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        }

        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        horizontalLine.setFont(parent.getFont());

        createPreferenceHeader(parent);

        return super.createDescriptionLabel(parent);
    }

    /*
     * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
     */
    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        configurationBlockControl = createPreferenceContent(composite);
        configurationBlockControl.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));

        if (isDataSourcePreferencePage()) {
            boolean useProjectSettings = hasDataSourceSpecificOptions(getDataSourceContainer());
            enableDataSourceSpecificSettings(useProjectSettings);
        }

        {
            DBPPreferenceStore store = getTargetPreferenceStore();
            loadPreferences(store);
        }

        Dialog.applyDialogFont(composite);
        return composite;
    }

    protected DBPPreferenceStore getTargetPreferenceStore() {
        return isDataSourcePreferencePage() ?
            getDataSourceContainer().getPreferenceStore() :
            DBWorkbench.getPlatform().getPreferenceStore();
    }

    private Link createLink(Composite composite, String text) {
        Link link = UIUtils.createLink(composite, "<A>" + text + "</A>", new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                doLinkActivated((Link) e.widget);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                widgetSelected(e);
            }
        });
        link.setFont(composite.getFont());
        return link;
    }

    protected void enableDataSourceSpecificSettings(boolean useProjectSpecificSettings) {
        dataSourceSettingsButton.setSelection(useProjectSpecificSettings);
        enablePreferenceContent(useProjectSpecificSettings);
        updateLinkVisibility();
        doStatusChanged();
    }

    protected void doStatusChanged() {
/*
        if (!isProjectPreferencePage() || useDataSourceSettings()) {
            updateStatus(fBlockStatus);
        } else {
            updateStatus(new StatusInfo());
        }
*/
    }

    protected void enablePreferenceContent(boolean enable) {
        if (enable) {
            if (blockEnableState != null) {
                blockEnableState.restore();
                blockEnableState = null;
            }
        } else {
            if (blockEnableState != null) {
                blockEnableState.restore();
            }
            blockEnableState = ControlEnableState.disable(configurationBlockControl);
        }
    }

    protected boolean useDataSourceSettings() {
        return isDataSourcePreferencePage() && dataSourceSettingsButton != null && dataSourceSettingsButton.getSelection();
    }

    private void updateLinkVisibility() {
        if (changeSettingsTargetLink == null || changeSettingsTargetLink.isDisposed()) {
            return;
        }

        if (isDataSourcePreferencePage()) {
            //changeSettingsTargetLink.setEnabled(!useDataSourceSettings());
            changeSettingsTargetLink.setEnabled(true);
        }
    }

    private void doLinkActivated(Link link) {
        PreferenceDialog prefDialog = null;
        if (isDataSourcePreferencePage()) {
            // Show global settings
            prefDialog = PreferencesUtil.createPreferenceDialogOn(
                getShell(),
                getPropertyPageID(),
                null,//new String[]{getPropertyPageID()},
                null);
        } else if (supportsDataSourceSpecificOptions()) {
            // Select datasource
            SelectDataSourceDialog dialog = new SelectDataSourceDialog(getShell(), null, null);
            if (dialog.open() != IDialogConstants.CANCEL_ID) {
                DBPDataSourceContainer dataSource = dialog.getDataSource();
                if (dataSource != null) {
                    DBNNode dsNode = DBNUtils.getNodeByObject(dataSource);
                    if (dsNode instanceof DBNDataSource) {
                        prefDialog = PreferencesUtil.createPropertyDialogOn(
                            getShell(),
                            dsNode,
                            getPropertyPageID(),
                            null,//new String[]{getPropertyPageID()},
                            null);
                    }
                }
            }
        }
        if (prefDialog != null) {

            prefDialog.open();
        }
    }

    @Override
    protected void performApply() {
        performOk();
    }

    @Override
    public final boolean performOk() {
        DBPPreferenceStore store = isDataSourcePreferencePage() ?
            getDataSourceContainer().getPreferenceStore() :
            DBWorkbench.getPlatform().getPreferenceStore();
        if (isDataSourcePreferencePage() && !useDataSourceSettings()) {
            // Just delete datasource specific settings
            clearPreferences(store);
            PrefUtils.savePreferenceStore(store);
        } else {
            savePreferences(store);
        }
        //PrefUtils.savePreferenceStore(store);
        return super.performOk();
    }

}
