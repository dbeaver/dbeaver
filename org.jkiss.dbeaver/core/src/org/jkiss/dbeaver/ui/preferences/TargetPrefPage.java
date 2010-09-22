/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * TargetPrefPage
 */
public abstract class TargetPrefPage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static final Log log = LogFactory.getLog(TargetPrefPage.class);

    private DBNDataSource containerNode;
    private Composite parentComposite;
    private Button dataSourceSettingsButton;
    private Control configurationBlockControl;
    private Link changeSettingsTargetLink;
    private ControlEnableState fBlockEnableState;

    protected TargetPrefPage()
    {
    }

    public final boolean isDataSourcePreferencePage()
    {
        return containerNode != null;
    }

    protected abstract boolean hasDataSourceSpecificOptions(DataSourceDescriptor project);

    protected abstract boolean supportsDataSourceSpecificOptions();

    protected abstract Control createPreferenceContent(Composite composite);

    protected abstract void loadPreferences(IPreferenceStore store);
    protected abstract void savePreferences(IPreferenceStore store);
    protected abstract void clearPreferences(IPreferenceStore store);

    protected abstract String getPropertyPageID();

    public DataSourceDescriptor getDataSourceContainer()
    {
        return containerNode.getObject();
    }

    public void init(IWorkbench workbench)
    {
    }

    public IAdaptable getElement()
    {
        return containerNode;
    }

    public void setElement(IAdaptable element)
    {
        containerNode = (DBNDataSource) element.getAdapter(DBNDataSource.class);
    }

    protected Label createDescriptionLabel(Composite parent)
    {
        parentComposite = parent;
        if (isDataSourcePreferencePage()) {
            Composite composite = new Composite(parent, SWT.NONE);
            composite.setFont(parent.getFont());
            GridLayout layout = new GridLayout();
            layout.marginHeight = 0;
            layout.marginWidth = 0;
            layout.numColumns = 2;
            composite.setLayout(layout);
            composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

            dataSourceSettingsButton = new Button(composite, SWT.CHECK);
            dataSourceSettingsButton.addSelectionListener(new SelectionAdapter()
            {
                public void widgetSelected(SelectionEvent e)
                {
                    boolean enabled = dataSourceSettingsButton.getSelection();
                    enableDataSourceSpecificSettings(enabled);
                }
            });
            dataSourceSettingsButton.setText("Use datasource settings");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            dataSourceSettingsButton.setLayoutData(gd);

            changeSettingsTargetLink = createLink(composite, "Show global settings");
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));

        } else if (supportsDataSourceSpecificOptions()) {
            changeSettingsTargetLink = createLink(parent, "Show datasource settings");
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        }

        Label horizontalLine = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
        horizontalLine.setFont(parent.getFont());

        return super.createDescriptionLabel(parent);
    }

    /*
     * @see org.eclipse.jface.preference.IPreferencePage#createContents(Composite)
     */
    protected Control createContents(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);
        composite.setFont(parent.getFont());

        GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);

        configurationBlockControl = createPreferenceContent(composite);
        configurationBlockControl.setLayoutData(data);

        if (isDataSourcePreferencePage()) {
            boolean useProjectSettings = hasDataSourceSpecificOptions(getDataSourceContainer());
            enableDataSourceSpecificSettings(useProjectSettings);
        }

        {
            IPreferenceStore store = useDataSourceSettings() ?
                getDataSourceContainer().getPreferenceStore() :
                DBeaverCore.getInstance().getGlobalPreferenceStore();
            loadPreferences(store);
        }

        Dialog.applyDialogFont(composite);
        return composite;
    }

    private Link createLink(Composite composite, String text)
    {
        Link link = new Link(composite, SWT.NONE);
        link.setFont(composite.getFont());
        link.setText("<A>" + text + "</A>");  //$NON-NLS-1$//$NON-NLS-2$
        link.addSelectionListener(new SelectionListener()
        {
            public void widgetSelected(SelectionEvent e)
            {
                doLinkActivated((Link) e.widget);
            }

            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }
        });
        return link;
    }

    protected void enableDataSourceSpecificSettings(boolean useProjectSpecificSettings)
    {
        dataSourceSettingsButton.setSelection(useProjectSpecificSettings);
        enablePreferenceContent(useProjectSpecificSettings);
        updateLinkVisibility();
        doStatusChanged();
    }

    protected void doStatusChanged()
    {
/*
        if (!isProjectPreferencePage() || useDataSourceSettings()) {
            updateStatus(fBlockStatus);
        } else {
            updateStatus(new StatusInfo());
        }
*/
    }

    protected void enablePreferenceContent(boolean enable)
    {
        if (enable) {
            if (fBlockEnableState != null) {
                fBlockEnableState.restore();
                fBlockEnableState = null;
            }
        } else {
            if (fBlockEnableState == null) {
                fBlockEnableState = ControlEnableState.disable(configurationBlockControl);
            }
        }
    }

    protected boolean useDataSourceSettings()
    {
        return isDataSourcePreferencePage() && dataSourceSettingsButton != null && dataSourceSettingsButton.getSelection();
    }

    private void updateLinkVisibility()
    {
        if (changeSettingsTargetLink == null || changeSettingsTargetLink.isDisposed()) {
            return;
        }

        if (isDataSourcePreferencePage()) {
            changeSettingsTargetLink.setEnabled(!useDataSourceSettings());
        }
    }

    private void doLinkActivated(Link link)
    {
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
            DataSourceDescriptor dataSource = SelectDataSourceDialog.selectDataSource(getShell());
            if (dataSource != null) {
                DBNNode dsNode = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dataSource);
                if (dsNode instanceof DBNDataSource) {
                    prefDialog = PreferencesUtil.createPropertyDialogOn(
                        getShell(),
                        (DBNDataSource)dsNode,
                        getPropertyPageID(),
                        null,//new String[]{getPropertyPageID()},
                        null);
                }
            }
        }
        if (prefDialog != null) {

            prefDialog.open();
        }
    }

    protected final void performApply()
    {
        performOk();
    }

    public final boolean performOk()
    {
        IPreferenceStore store = isDataSourcePreferencePage() ?
            getDataSourceContainer().getPreferenceStore() :
            DBeaverCore.getInstance().getGlobalPreferenceStore();
        if (isDataSourcePreferencePage() && !useDataSourceSettings()) {
            // Just delete datasource specific settings
            clearPreferences(store);
        } else {
            savePreferences(store);
        }
        DBeaverUtils.savePreferenceStore(store);
        return super.performOk();
    }

}
