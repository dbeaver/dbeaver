/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

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
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.LocaleSelectorControl;
import org.jkiss.dbeaver.ui.controls.proptree.EditablePropertiesControl;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.DBeaverUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public PrefPageDataFormat()
    {
        super();
        setPreferenceStore(DBeaverCore.getInstance().getGlobalPreferenceStore());
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return false
            //store.contains(PrefConstants.RESULT_SET_MAX_ROWS) ||
            //store.contains(PrefConstants.QUERY_ROLLBACK_ON_ERROR) ||
            //store.contains(PrefConstants.DEFAULT_AUTO_COMMIT)
            ;
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        boldFont = UIUtils.makeBoldFont(parent.getFont());

        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

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

            UIUtils.createControlLabel(formatGroup, "Sample");
            sampleText = new Text(formatGroup, SWT.BORDER | SWT.READ_ONLY);
            sampleText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }

        return composite;
    }

    protected void loadPreferences(IPreferenceStore store)
    {
        formatterProfile = DBeaverCore.getInstance().getDataSourceRegistry().loadDataFormatterProfile(store);
        try {
            // Set locale
            localeSelector.setLocale(formatterProfile.getLocale());
            // Load types
            formatterDescriptors = new ArrayList<DataFormatterDescriptor>(DBeaverCore.getInstance().getDataSourceRegistry().getDataFormatters());
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

        Map<String,String> formatterProps = formatterProfile.getFormatterProperties(formatterDescriptor.getId());
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
            DBDDataFormatter formatter = formatterProfile.createFormatter(formatterDescriptor.getId());
            String sampleValue = formatter.formatValue(formatterDescriptor.getSample().getSampleValue());
            sampleText.setText(sampleValue);
        } catch (Exception e) {
            log.warn("Could not render sample value", e);
        }
    }

    private void onLocaleChange(Locale locale)
    {
        formatterProfile.setLocale(locale);
        if (propertiesControl.isDirty()) {
            reloadFormatter();
        } else {
            reloadSample();
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            formatterProfile.saveProfile(store);
            //store.setValue(PrefConstants.DEFAULT_AUTO_COMMIT, autoCommitCheck.getSelection());
            //store.setValue(PrefConstants.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            //store.setValue(PrefConstants.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        DBeaverUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        //store.setToDefault(PrefConstants.DEFAULT_AUTO_COMMIT);
        //store.setToDefault(PrefConstants.QUERY_ROLLBACK_ON_ERROR);
        //store.setToDefault(PrefConstants.RESULT_SET_MAX_ROWS);
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
}