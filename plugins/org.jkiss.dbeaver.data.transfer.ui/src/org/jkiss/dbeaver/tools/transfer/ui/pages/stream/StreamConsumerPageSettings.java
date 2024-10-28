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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.Section;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.*;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.internal.UIConnectionMessages;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class StreamConsumerPageSettings extends DataTransferPageNodeSettings {

    private static final Log log = Log.getLog(StreamConsumerPageSettings.class);

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;
    private static final int LOB_ENCODING_NATIVE = 3;

    private PropertyTreeViewer propsEditor;
    private Combo lobExtractType;
    private Label lobEncodingLabel;
    private Combo lobEncodingCombo;
    private Combo formatProfilesCombo;
    private PropertySourceCustom propertySource;
    private ValueFormatSelector valueFormatSelector;

    public StreamConsumerPageSettings() {
        super(DTMessages.data_transfer_wizard_settings_name);
        setTitle(DTUIMessages.stream_consumer_page_settings_title);
        setDescription(DTUIMessages.stream_consumer_page_settings_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        DBPDataFormatterRegistry dataFormatterRegistry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();

        initializeDialogUnits(parent);
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite generalSettings = UIUtils.createComposite(composite, 3);
            formatProfilesCombo = UIUtils.createLabelCombo(generalSettings, DTMessages.data_transfer_wizard_settings_label_formatting, SWT.DROP_DOWN | SWT.READ_ONLY);
            GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
            formatProfilesCombo.setLayoutData(gd);
            formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    if (formatProfilesCombo.getSelectionIndex() > 0) {
                        settings.setFormatterProfile(
                            dataFormatterRegistry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
                    } else {
                        settings.setFormatterProfile(null);
                    }
                }
            });

            Button editProfileButton = UIUtils.createDialogButton(
                generalSettings,
                DTMessages.data_transfer_wizard_settings_button_edit,
                new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        PreferenceDialog propDialog = PreferencesUtil.createPropertyDialogOn(
                            getShell(),
                            dataFormatterRegistry,
                            "org.jkiss.dbeaver.preferences.main.dataformat", // TODO: replace this hardcode with some model invocation
                            null,
                            getSelectedFormatterProfile(),
                            PreferencesUtil.OPTION_NONE);
                        if (propDialog != null) {
                            propDialog.open();
                            reloadFormatProfiles();
                        }
                    }
                }
            );
            editProfileButton.setEnabled(true);

            reloadFormatProfiles();
        }

        {
            final ExpandableComposite generalExpander = new ExpandableComposite(composite, SWT.NONE, Section.TREE_NODE);
            generalExpander.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
            generalExpander.setText(UIConnectionMessages.dialog_connection_advanced_settings);
            generalExpander.addExpansionListener(new ExpansionAdapter() {
                @Override
                public void expansionStateChanged(ExpansionEvent e) {
                    UIUtils.resizeShell(parent.getShell());
                }
            });

            Composite generalSettings = UIUtils.createControlGroup(generalExpander, DTMessages.data_transfer_wizard_settings_group_general, 5, GridData.HORIZONTAL_ALIGN_BEGINNING, -1);
            //generalSettings.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));

            generalExpander.setClient(generalSettings);
            {
                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_binaries);
                Composite binariesPanel = UIUtils.createComposite(generalSettings, 4);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 4;
                binariesPanel.setLayoutData(gd);
                lobExtractType = new Combo(binariesPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobExtractType.setItems(
                    DTMessages.data_transfer_wizard_settings_binaries_item_set_to_null,
                    DTMessages.data_transfer_wizard_settings_binaries_item_save_to_file,
                    DTMessages.data_transfer_wizard_settings_binaries_item_inline);
                lobExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (lobExtractType.getSelectionIndex()) {
                            case EXTRACT_LOB_SKIP: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.SKIP); break;
                            case EXTRACT_LOB_FILES: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.FILES); break;
                            case EXTRACT_LOB_INLINE: settings.setLobExtractType(StreamConsumerSettings.LobExtractType.INLINE); break;
                        }
                        updatePageCompletion();
                    }
                });

                lobEncodingLabel = UIUtils.createControlLabel(binariesPanel, DTMessages.data_transfer_wizard_settings_label_encoding);
                lobEncodingCombo = new Combo(binariesPanel, SWT.DROP_DOWN | SWT.READ_ONLY);
                lobEncodingCombo.setItems(
                    "Base64", //$NON-NLS-1$
                    "Hex", //$NON-NLS-1$
                    "Binary", //$NON-NLS-1$
                    "Native"); //$NON-NLS-1$
                lobEncodingCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (lobEncodingCombo.getSelectionIndex()) {
                            case LOB_ENCODING_BASE64: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BASE64); break;
                            case LOB_ENCODING_HEX: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.HEX); break;
                            case LOB_ENCODING_BINARY: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.BINARY); break;
                            case LOB_ENCODING_NATIVE: settings.setLobEncoding(StreamConsumerSettings.LobEncoding.NATIVE); break;
                        }
                    }
                });

                valueFormatSelector = new ValueFormatSelector(generalSettings);
                valueFormatSelector.select(settings.getValueFormat());
                valueFormatSelector.getCombo().addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setValueFormat(valueFormatSelector.getSelection());
                    }
                });
                valueFormatSelector.getCombo().setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 4, 1));

                UIUtils.createControlLabel(generalSettings, DTUIMessages.stream_consumer_page_mapping_label_configure);
                final Button button = UIUtils.createDialogButton(generalSettings, DTUIMessages.stream_consumer_page_mapping_button_configure, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent event) {
                        final List<StreamMappingContainer> mappings = new ArrayList<>();

                        try {
                            UIUtils.runInProgressDialog(monitor -> refreshMappings(monitor, mappings));
                        } catch (InvocationTargetException e) {
                            DBWorkbench.getPlatformUI().showError(
                                DTMessages.stream_transfer_consumer_title_configuration_load_failed,
                                DTMessages.stream_transfer_consumer_message_cannot_load_configuration,
                                e
                            );
                        }

                        new ConfigureColumnsPopup(getShell(), mappings, settings).open();
                    }
                });
                //((GridData) button.getLayoutData()).horizontalSpan = 4;
            }
        }

        {
            Composite exporterSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_settings_group_exporter, 1, GridData.FILL_BOTH, 0);

            propsEditor = new PropertyTreeViewer(exporterSettings, SWT.BORDER);
        }

        setControl(composite);
    }

    private Object getSelectedFormatterProfile()
    {
        DBPDataFormatterRegistry registry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();
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
        DBPDataFormatterRegistry registry = DBPPlatformDesktop.getInstance().getDataFormatterRegistry();
        formatProfilesCombo.removeAll();
        formatProfilesCombo.add(DTMessages.data_transfer_wizard_settings_listbox_formatting_item_default);
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
        getWizard().loadNodeSettings();

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();
        propertySource = new PropertySourceCustom(
            processor.getProperties(),
            getWizard().getSettings().getProcessorProperties());
        propsEditor.loadProperties(propertySource);

        switch (settings.getLobExtractType()) {
            case SKIP: lobExtractType.select(EXTRACT_LOB_SKIP); break;
            case FILES: lobExtractType.select(EXTRACT_LOB_FILES); break;
            case INLINE: lobExtractType.select(EXTRACT_LOB_INLINE); break;
        }
        switch (settings.getLobEncoding()) {
            case BASE64: lobEncodingCombo.select(LOB_ENCODING_BASE64); break;
            case HEX: lobEncodingCombo.select(LOB_ENCODING_HEX); break;
            case BINARY: lobEncodingCombo.select(LOB_ENCODING_BINARY); break;
            case NATIVE: lobEncodingCombo.select(LOB_ENCODING_NATIVE); break;
        }

        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        propsEditor.saveEditorValues();
        getWizard().getSettings().setProcessorProperties(propertySource.getPropertiesWithDefaults());
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        int selectionIndex = lobExtractType.getSelectionIndex();
        if (selectionIndex == EXTRACT_LOB_INLINE) {
            lobEncodingLabel.setVisible(true);
            lobEncodingCombo.setVisible(true);
        } else {
            lobEncodingLabel.setVisible(false);
            lobEncodingCombo.setVisible(false);
        }

        return true;
    }

    private static class ConfigureColumnsPopup extends BaseDialog {
        private final List<StreamMappingContainer> mappings;
        private final StreamConsumerSettings settings;

        private TreeViewer viewer;
        private CLabel errorLabel;

        public ConfigureColumnsPopup(@NotNull Shell shell, @NotNull List<StreamMappingContainer> mappings, @NotNull StreamConsumerSettings settings) {
            super(shell, DTUIMessages.stream_consumer_page_mapping_title, null);
            this.settings = settings;
            this.setShellStyle(SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL);
            this.mappings = mappings;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite group = super.createDialogArea(parent);

            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
            gd.widthHint = 400;
            gd.heightHint = 450;

            Composite composite = UIUtils.createComposite(group, 1);
            composite.setLayoutData(gd);

            viewer = new TreeViewer(composite, SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION);
            viewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
            viewer.getTree().setLinesVisible(true);
            viewer.getTree().setHeaderVisible(true);
            viewer.getTree().setLayoutData(gd);

            viewer.setContentProvider(new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object element) {
                    // We have preloaded the attributes before, so it is 'safe' to use void monitor here
                    return ((StreamMappingContainer) element).getAttributes(new VoidProgressMonitor()).toArray();
                }

                @Override
                public boolean hasChildren(Object element) {
                    return element instanceof StreamMappingContainer;
                }
            });

            {
                TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
                column.setLabelProvider(new CellLabelProvider() {
                    @Override
                    public void update(ViewerCell cell) {
                        final Object element = cell.getElement();
                        final DBPNamedObject object = (DBPNamedObject) element;
                        cell.setText(object.getName());
                        cell.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(object)));
                    }
                });
                column.getColumn().setText(DTUIMessages.stream_consumer_page_mapping_name_column_name);
            }

            {
                TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.LEFT);
                column.setLabelProvider(new CellLabelProvider() {
                    @Override
                    public void update(ViewerCell cell) {
                        final Object element = cell.getElement();
                        if (element instanceof StreamMappingAttribute attribute) {
                            cell.setText(attribute.getMappingType().name());
                            cell.setBackground(attribute.getContainer().isComplete() ? null : UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                        } else if (element instanceof StreamMappingContainer container) {
                            final StreamMappingType type = container.getMappingType();
                            cell.setText(type != null ? type.name() : "");
                            cell.setBackground(container.isComplete() ? null : UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
                        }
                    }
                });
                column.setEditingSupport(new EditingSupport(viewer) {
                    @Override
                    protected CellEditor getCellEditor(Object element) {
                        final String[] items = {
                            StreamMappingType.export.name(),
                            StreamMappingType.skip.name()
                        };

                        return new CustomComboBoxCellEditor(
                            viewer,
                            viewer.getTree(),
                            items,
                            SWT.DROP_DOWN | SWT.READ_ONLY
                        );
                    }

                    @Override
                    protected boolean canEdit(Object element) {
                        return true;
                    }

                    @Override
                    protected Object getValue(Object element) {
                        if (element instanceof StreamMappingAttribute attribute) {
                            return attribute.getMappingType().name();
                        } else if (element instanceof StreamMappingContainer container) {
                            final StreamMappingType type = container.getMappingType();
                            return type != null ? type.name() : null;
                        } else {
                            return null;
                        }
                    }

                    @Override
                    protected void setValue(Object element, Object value) {
                        if (((String) value).isEmpty()) {
                            return;
                        }
                        final StreamMappingType type = StreamMappingType.valueOf(value.toString());
                        if (element instanceof StreamMappingAttribute attribute) {
                            attribute.setMappingType(type);
                        } else if (element instanceof StreamMappingContainer container) {
                            container.setMappingType(type);
                        }
                        viewer.refresh();
                        updateCompletion();
                    }
                });
                column.getColumn().setText(DTUIMessages.stream_consumer_page_mapping_mapping_column_name);
            }

            errorLabel = new CLabel(group, SWT.NONE);
            errorLabel.setText(DTUIMessages.stream_consumer_page_mapping_label_error_no_columns_selected_text);
            errorLabel.setImage(DBeaverIcons.getImage(DBIcon.SMALL_ERROR));
            errorLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));

            UIUtils.asyncExec(() -> {
                viewer.setInput(mappings);
                viewer.expandAll(true);
                UIUtils.packColumns(viewer.getTree(), true, new float[]{0.75f, 0.25f});
                updateCompletion();
            });

            return group;
        }

        @Override
        protected void okPressed() {
            settings.getDataMappings().clear();

            for (StreamMappingContainer mapping : mappings) {
                settings.addDataMapping(mapping);
            }

            super.okPressed();
        }

        private void updateCompletion() {
            final boolean isComplete = mappings.stream().allMatch(StreamMappingContainer::isComplete);
            final Button okButton = getButton(IDialogConstants.OK_ID);
            errorLabel.setVisible(!isComplete);
            okButton.setEnabled(isComplete);
        }
    }

    private void refreshMappings(@NotNull DBRProgressMonitor monitor, @NotNull List<StreamMappingContainer> mappings) {
        final StreamConsumerSettings settings = getWizard().getPageSettings(StreamConsumerPageSettings.this, StreamConsumerSettings.class);
        final List<DataTransferPipe> pipes = getWizard().getSettings().getDataPipes();

        try {
            monitor.beginTask("Load mappings", pipes.size());
            for (DataTransferPipe pipe : pipes) {
                DBSDataContainer source = (DBSDataContainer) pipe.getProducer().getDatabaseObject();
                StreamMappingContainer mapping = settings.getDataMapping(source);

                if (mapping == null) {
                    mapping = new StreamMappingContainer(source);

                    for (StreamMappingAttribute attribute : mapping.getAttributes(monitor)) {
                        attribute.setMappingType(StreamMappingType.export);
                    }
                } else {
                    // Create a copy to avoid direct modifications
                    mapping = new StreamMappingContainer(mapping);
                }

                mappings.add(mapping);
                monitor.worked(1);
            }
        } finally {
            monitor.done();
        }
    }

    @Override
    public boolean isPageApplicable() {
        return isConsumerOfType(StreamTransferConsumer.class);
    }

}