/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com)
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
package org.jkiss.dbeaver.data.office.export;

import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.registry.formatter.DataFormatterRegistry;
import org.jkiss.dbeaver.registry.transfer.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerPageSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.PrefPageDataFormat;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

/**
 * @author Andrey.Hitrin
 *
 */



public class StreamPOIConsumerPageSettings extends StreamConsumerPageSettings {
	
	private PropertyTreeViewer propsEditor;
	private Combo formatProfilesCombo;
	private PropertySourceCustom propertySource;
	
	
	 public StreamPOIConsumerPageSettings() {
	        super();
	        setTitle(CoreMessages.data_transfer_wizard_settings_title);
	        setDescription(CoreMessages.data_transfer_wizard_settings_description);
	        setPageComplete(false);
	    }
	 
	 
	  @Override
	    public void createControl(Composite parent) {
	        initializeDialogUnits(parent);
	        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
	        Composite composite = new Composite(parent, SWT.NULL);
	        GridLayout gl = new GridLayout();
	        gl.marginHeight = 0;
	        gl.marginWidth = 0;
	        composite.setLayout(gl);
	        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

	        {
	            Group generalSettings = new Group(composite, SWT.NONE);
	            generalSettings.setText(CoreMessages.data_transfer_wizard_settings_group_general);
	            gl = new GridLayout(4, false);
	            generalSettings.setLayout(gl);
	            generalSettings.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	            {
	                Composite formattingGroup = UIUtils.createPlaceholder(generalSettings, 3);
	                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
	                gd.horizontalSpan = 4;
	                formattingGroup.setLayoutData(gd);
	                
	                UIUtils.createControlLabel(formattingGroup, CoreMessages.data_transfer_wizard_settings_label_formatting);
	                formatProfilesCombo = new Combo(formattingGroup, SWT.DROP_DOWN | SWT.READ_ONLY);
	                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
	                gd.widthHint = 200;
	                formatProfilesCombo.setLayoutData(gd);
	                formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
	                    @Override
	                    public void widgetSelected(SelectionEvent e)
	                    {
	                        if (formatProfilesCombo.getSelectionIndex() > 0) {
	                            settings.setFormatterProfile(
	                                DataFormatterRegistry.getInstance().getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
	                        } else {
	                            settings.setFormatterProfile(null);
	                        }
	                    }
	                });

	                Button profilesManageButton = new Button(formattingGroup, SWT.PUSH);
	                profilesManageButton.setText(CoreMessages.data_transfer_wizard_settings_button_edit);
	                profilesManageButton.addSelectionListener(new SelectionAdapter() {
	                    @Override
	                    public void widgetSelected(SelectionEvent e)
	                    {
	                        PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
	                            getShell(),
	                            DataFormatterRegistry.getInstance(),
	                            PrefPageDataFormat.PAGE_ID,
	                            null,
	                            getSelectedFormatterProfile(),
	                            PreferencesUtil.OPTION_NONE);
	                        if (propDialog != null) {
	                            propDialog.open();
	                            reloadFormatProfiles();
	                        }
	                    }
	                });

	                reloadFormatProfiles();
	            }
	        }

	        Group exporterSettings = new Group(composite, SWT.NONE);
	        exporterSettings.setText(CoreMessages.data_transfer_wizard_settings_group_exporter);
	        exporterSettings.setLayoutData(new GridData(GridData.FILL_BOTH));
	        exporterSettings.setLayout(new GridLayout(1, false));
	        
	        propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);

	        setControl(composite);
	    }

	    private Object getSelectedFormatterProfile()
	    {
	        DataFormatterRegistry registry = DataFormatterRegistry.getInstance();
	        int selectionIndex = formatProfilesCombo.getSelectionIndex();
	        if (selectionIndex < 0) {
	            return null;
	        } else if (selectionIndex == 0) {
	            return registry.getGlobalProfile();
	        } else {
	            return registry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo));
	        }
	    }

	    private void reloadFormatProfiles()
	    {
	        DataFormatterRegistry registry = DataFormatterRegistry.getInstance();
	        formatProfilesCombo.removeAll();
	        formatProfilesCombo.add(CoreMessages.data_transfer_wizard_settings_listbox_formatting_item_default);
	        for (DBDDataFormatterProfile profile : registry.getCustomProfiles()) {
	            formatProfilesCombo.add(profile.getProfileName());
	        }
	        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
	        DBDDataFormatterProfile formatterProfile = settings.getFormatterProfile();
	        if (formatterProfile != null) {
	            if (!UIUtils.setComboSelection(formatProfilesCombo, formatterProfile.getProfileName())) {
	                formatProfilesCombo.select(0);
	            }
	        } else {
	            formatProfilesCombo.select(0);
	        }
	    }

	    @Override
	    public void activatePage() {
	        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

	        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
	        propertySource = new PropertySourceCustom(
	            processor.getProperties(),
	            getWizard().getSettings().getProcessorProperties());
	        propsEditor.loadProperties(propertySource);
	        updatePageCompletion();
	        
	    }

	    @Override
	    public void deactivatePage()
	    {
	    	getWizard().getSettings().setProcessorProperties(propertySource.getPropertiesWithDefaults());
	    }

	    @Override
	    protected boolean determinePageCompletion()
	    {

	        return true;
	    }
}
