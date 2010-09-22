/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.DataFormatterDescriptor;
import org.jkiss.dbeaver.registry.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.LocaleSelectorControl;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.*;
import java.util.List;

/**
 * PrefPageDataFormat
 */
public class PrefPageDataFormat extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.dataformat";

    private DBDDataFormatterProfile formatterProfile;

    private Font boldFont;
    private Combo typeCombo;
    private EditablePropertiesControl propertiesControl;
    private Text sampleText;

    private List<DataFormatterDescriptor> formatterDescriptors;
    private LocaleSelectorControl localeSelector;

    private String profileName;
    private Locale profileLocale;
    private Map<String, Map<String, String>> profileProperties = new HashMap<String, Map<String, String>>();
    private Combo profilesCombo;

    public PrefPageDataFormat()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        return dataSourceDescriptor.getDataFormatterProfile().isOverridesParent();
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        {
            Composite profileGroup = UIUtils.createPlaceholder(composite, 3);
            profileGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            UIUtils.createControlLabel(profileGroup, "Profile");
            profilesCombo = new Combo(profileGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            profilesCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            Button editButton = new Button(profileGroup, SWT.PUSH);
            editButton.setText("Edit Profiles");
            editButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    managerProfiles();
                }
            });
        }

        // Locale
        localeSelector = new LocaleSelectorControl(composite, null);
        localeSelector.addListener(SWT.Selection, new Listener() {
            public void handleEvent(Event event)
            {
                if (event.data instanceof Locale) {
                    onLocaleChange((Locale) event.data);
                }
            }
        });

        // formats
        {
            Group formatGroup = new Group(composite, SWT.NONE);
            formatGroup.setText("Format");
            formatGroup.setLayout(new GridLayout(2, false));
            formatGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            UIUtils.createControlLabel(formatGroup, "Type");
            typeCombo = new Combo(formatGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
            typeCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    reloadFormatter();
                }
            });

            Label propsLabel = UIUtils.createControlLabel(formatGroup, "Settings");
            propsLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            propertiesControl = new EditablePropertiesControl(formatGroup, SWT.NONE);
            propertiesControl.setMarginVisible(false);
            propertiesControl.setLayoutData(new GridData(GridData.FILL_BOTH));
            propertiesControl.addListener(SWT.Modify, new Listener() {
                public void handleEvent(Event event)
                {
                    saveFormatterProperties();
                }
            });

            UIUtils.createControlLabel(formatGroup, "Sample");
            sampleText = new Text(formatGroup, SWT.BORDER | SWT.READ_ONLY);
            sampleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return composite;
    }

    private void managerProfiles()
    {
        ManagerProfilesDialog dialog = new ManagerProfilesDialog(getShell());
        if (dialog.open() == IDialogConstants.OK_ID) {
            
        }
    }

    private DataFormatterDescriptor getCurrentFormatter()
    {
        int selectionIndex = typeCombo.getSelectionIndex();
        if (selectionIndex < 0) {
            return null;
        }
        return formatterDescriptors.get(selectionIndex);
    }

    private void reloadFormatter()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }

        Map<String,String> formatterProps = profileProperties.get(formatterDescriptor.getId());
        Map<String, String> defaultProps = formatterDescriptor.getSample().getDefaultProperties(localeSelector.getSelectedLocale());
        propertiesControl.loadProperties(
            formatterDescriptor.getPropertyGroups(),
            formatterProps,
            defaultProps);
        reloadSample();
    }

    private void reloadSample()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }
        try {
            DBDDataFormatter formatter = formatterDescriptor.createFormatter();

            Map<String, String> defProps = formatterDescriptor.getSample().getDefaultProperties(profileLocale);
            Map<String, String> props = profileProperties.get(formatterDescriptor.getId());
            Map<String, String> formatterProps = new HashMap<String, String>();
            if (defProps != null && !defProps.isEmpty()) {
                formatterProps.putAll(defProps);
            }
            if (props != null && !props.isEmpty()) {
                formatterProps.putAll(props);
            }
            formatter.init(profileLocale, formatterProps);

            String sampleValue = formatter.formatValue(formatterDescriptor.getSample().getSampleValue());
            sampleText.setText(sampleValue);
        } catch (Exception e) {
            log.warn("Could not render sample value", e);
        }
    }

    private void saveFormatterProperties()
    {
        DataFormatterDescriptor formatterDescriptor = getCurrentFormatter();
        if (formatterDescriptor == null) {
            return;
        }
        Map<String, String> props = propertiesControl.getProperties();
        profileProperties.put(formatterDescriptor.getId(), props);
        reloadSample();
    }

    private void onLocaleChange(Locale locale)
    {
        if (!locale.equals(profileLocale)) {
            profileLocale = locale;
            DataFormatterDescriptor formatter = getCurrentFormatter();
            if (formatter != null) {
                propertiesControl.reloadDefaultValues(formatter.getSample().getDefaultProperties(locale));
            }
            reloadSample();
        }
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        DataFormatterRegistry formatterRegistry = DBeaverCore.getInstance().getDataFormatterRegistry();

        if (isDataSourcePreferencePage()) {
            formatterProfile = getDataSourceContainer().getDataFormatterProfile();
        } else {
            formatterProfile = formatterRegistry.getGlobalProfile();
        }
        refreshProfileList();

        formatterDescriptors = new ArrayList<DataFormatterDescriptor>(formatterRegistry.getDataFormatters());

        profileName = formatterProfile.getProfileName();
        profileLocale = formatterProfile.getLocale();
        for (DataFormatterDescriptor dfd : formatterDescriptors) {
            Map<String, String> formatterProps = formatterProfile.getFormatterProperties(dfd.getId());
            if (formatterProps != null) {
                profileProperties.put(dfd.getId(), formatterProps);
            }
        }

        try {
            // Set locale
            localeSelector.setLocale(profileLocale);
            // Load types
            for (DataFormatterDescriptor formatter : formatterDescriptors) {
                typeCombo.add(formatter.getName());
            }
            if (typeCombo.getItemCount() > 0) {
                typeCombo.select(0);
            }
            reloadFormatter();
            //autoCommitCheck.setSelection(store.getBoolean(PrefConstants.DEFAULT_AUTO_COMMIT));
            //rollbackOnErrorCheck.setSelection(store.getBoolean(PrefConstants.QUERY_ROLLBACK_ON_ERROR));
            //resultSetSize.setSelection(store.getInt(PrefConstants.RESULT_SET_MAX_ROWS));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    private void refreshProfileList()
    {
        String defProfileName;
        if (isDataSourcePreferencePage()) {
            defProfileName = "Connection '" + getDataSourceContainer().getName() + "'";
        } else {
            defProfileName = "Global";
        }
        profilesCombo.removeAll();
        profilesCombo.add("<" + defProfileName + ">");
        profilesCombo.select(0);
        if (profilesCombo.getItemCount() < 2) {
            profilesCombo.setEnabled(false);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            formatterProfile.setProfileName(profileName);
            formatterProfile.setLocale(profileLocale);
            for (String typeId : profileProperties.keySet()) {
                formatterProfile.setFormatterProperties(typeId, profileProperties.get(typeId));
            }
            formatterProfile.saveProfile();
        } catch (Exception e) {
            log.warn(e);
        }
        DBeaverUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        formatterProfile.reset();
    }

    public void applyData(Object data)
    {
        super.applyData(data);
    }

    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

    @Override
    public void dispose()
    {
        boldFont.dispose();
        super.dispose();
    }

    class ManagerProfilesDialog extends org.eclipse.jface.dialogs.Dialog {

        private ManagerProfilesDialog(Shell parentShell)
        {
            super(parentShell);
        }

        protected boolean isResizable()
        {
            return true;
        }

        protected Control createDialogArea(Composite parent)
        {
            getShell().setText("Manage profiles");

            Composite group = new Composite(parent, SWT.NONE);
            group.setLayout(new GridLayout(1, false));
            group.setLayoutData(new GridData(GridData.FILL_BOTH));

            org.eclipse.swt.widgets.List profileList = new org.eclipse.swt.widgets.List(group, SWT.SINGLE | SWT.BORDER);
            profileList.setLayoutData(new GridData(GridData.FILL_BOTH));

            return parent;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            createButton(parent, 0, "New Profile", false);
            createButton(parent, 0, "Delete Profile", false);
            createButton(
                parent,
                IDialogConstants.OK_ID,
                IDialogConstants.CLOSE_LABEL,
                true);
        }

    }

}