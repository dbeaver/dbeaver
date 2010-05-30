/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;

import java.io.IOException;

/**
 * TargetPrefPage
 */
public abstract class TargetPrefPage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    static Log log = LogFactory.getLog(TargetPrefPage.class);

    private DataSourceDescriptor dataSource;
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
        return dataSource != null;
    }

    protected abstract boolean hasDataSourceSpecificOptions(DataSourceDescriptor project);

    protected abstract boolean supportsDataSourceSpecificOptions();

    protected abstract Control createPreferenceContent(Composite composite);

    protected abstract void loadPreferences(IPreferenceStore store);
    protected abstract void savePreferences(IPreferenceStore store);
    protected abstract void clearPreferences(IPreferenceStore store);

    protected abstract String getPropertyPageID();

    public DataSourceDescriptor getDataSource()
    {
        return dataSource;
    }

    public void init(IWorkbench workbench)
    {
    }

    public IAdaptable getElement()
    {
        return dataSource;
    }

    public void setElement(IAdaptable element)
    {
        dataSource = (DataSourceDescriptor) element.getAdapter(DBSDataSourceContainer.class);
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

            Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
            horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
            horizontalLine.setFont(composite.getFont());
        } else if (supportsDataSourceSpecificOptions()) {
            changeSettingsTargetLink = createLink(parent, "Show datasource settings");
            changeSettingsTargetLink.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
        }

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
            boolean useProjectSettings = hasDataSourceSpecificOptions(getDataSource());
            enableDataSourceSpecificSettings(useProjectSettings);
        }

        {
            IPreferenceStore store = useDataSourceSettings() ?
                dataSource.getPreferenceStore() :
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
        if (isDataSourcePreferencePage()) {
            PreferencesUtil.createPreferenceDialogOn(
                getShell(),
                getPropertyPageID(),
                null,//new String[]{getPropertyPageID()},
                null).open();
        } else if (supportsDataSourceSpecificOptions()) {
            // Select datasource
            DataSourceDescriptor dataSource = SelectDataSourceDialog.selectDataSource(getShell());
            if (dataSource != null) {
                PreferencesUtil.createPropertyDialogOn(
                    getShell(),
                    dataSource,
                    getPropertyPageID(),
                    null,//new String[]{getPropertyPageID()},
                    null).open();
            }
        }
    }

    protected final void performApply()
    {
        performOk();
    }

    public final boolean performOk()
    {
        IPreferenceStore store = isDataSourcePreferencePage() ?
            dataSource.getPreferenceStore() :
            DBeaverCore.getInstance().getGlobalPreferenceStore();
        if (isDataSourcePreferencePage() && !useDataSourceSettings()) {
            // Just delete datasource specific settings
            clearPreferences(store);
        } else {
            savePreferences(store);
        }
        if (store instanceof IPersistentPreferenceStore) {
            try {
                ((IPersistentPreferenceStore)store).save();
            } catch (IOException ex) {
                log.warn("Error saving preferences", ex);
            }
        }
        return super.performOk();
    }

}
