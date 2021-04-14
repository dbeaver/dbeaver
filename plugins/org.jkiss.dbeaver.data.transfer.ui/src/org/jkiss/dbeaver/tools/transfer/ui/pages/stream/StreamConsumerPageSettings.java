/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.app.DBPDataFormatterRegistry;
import org.jkiss.dbeaver.model.data.DBDDataFormatterProfile;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingAttribute;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingContainer;
import org.jkiss.dbeaver.tools.transfer.stream.StreamMappingType;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SharedTextColors;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomComboBoxCellEditor;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.ValueFormatSelector;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class StreamConsumerPageSettings extends ActiveWizardPage<DataTransferWizard> {

    private static final Log log = Log.getLog(StreamConsumerPageSettings.class);

    private static final int EXTRACT_LOB_SKIP = 0;
    private static final int EXTRACT_LOB_FILES = 1;
    private static final int EXTRACT_LOB_INLINE = 2;

    private static final int LOB_ENCODING_BASE64 = 0;
    private static final int LOB_ENCODING_HEX = 1;
    private static final int LOB_ENCODING_BINARY = 2;
    private static final int LOB_ENCODING_NATIVE = 3;

    private final List<StreamMappingContainer> mappings = new ArrayList<>();
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

        DBPDataFormatterRegistry dataFormatterRegistry = DBWorkbench.getPlatform().getDataFormatterRegistry();

        initializeDialogUnits(parent);
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        Composite composite = UIUtils.createComposite(parent, 1);

        {
            Composite generalSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_settings_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            {
                formatProfilesCombo = UIUtils.createLabelCombo(generalSettings, DTMessages.data_transfer_wizard_settings_label_formatting, SWT.DROP_DOWN | SWT.READ_ONLY);
                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 3;
                formatProfilesCombo.setLayoutData(gd);
                formatProfilesCombo.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        if (formatProfilesCombo.getSelectionIndex() > 0) {
                            settings.setFormatterProfile(
                                dataFormatterRegistry.getCustomProfile(UIUtils.getComboSelection(formatProfilesCombo)));
                        } else {
                            settings.setFormatterProfile(null);
                        }
                    }
                });

                UIUtils.createDialogButton(generalSettings, DTMessages.data_transfer_wizard_settings_button_edit, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
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
                });

                reloadFormatProfiles();

                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_settings_label_binaries);
                Composite binariesPanel = UIUtils.createComposite(generalSettings, 4);
                gd = new GridData(GridData.FILL_HORIZONTAL);
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
                valueFormatSelector.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setValueFormat(valueFormatSelector.getSelection());
                    }
                });

                {
                    Composite columnsPanel = UIUtils.createComposite(generalSettings, 5);
                    columnsPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));

                    UIUtils.createDialogButton(columnsPanel, DTUIMessages.stream_consumer_page_mapping_button_configure, new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent event) {
                            if (mappings.isEmpty()) {
                                try {
                                    getWizard().getRunnableContext().run(true, true, monitor -> {
                                        refreshMappings(monitor);
                                        UIUtils.asyncExec(() -> new ConfigureColumnsPopup(getShell()).open());
                                    });
                                } catch (InvocationTargetException e) {
                                    DBWorkbench.getPlatformUI().showError(
                                        DTMessages.stream_transfer_consumer_title_configuration_load_failed,
                                        DTMessages.stream_transfer_consumer_message_cannot_load_configuration,
                                        e
                                    );
                                } catch (InterruptedException e) {
                                    log.debug("Canceled by user", e);
                                }
                            } else {
                                new ConfigureColumnsPopup(getShell()).open();
                            }
                        }
                    });
                }
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
        DBPDataFormatterRegistry registry = DBWorkbench.getPlatform().getDataFormatterRegistry();
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
        DBPDataFormatterRegistry registry = DBWorkbench.getPlatform().getDataFormatterRegistry();
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

    private final class ConfigureColumnsPopup extends BaseDialog {
        private TreeViewer viewer;
        private CLabel errorLabel;

        public ConfigureColumnsPopup(@NotNull Shell shell) {
            super(shell, DTUIMessages.stream_consumer_page_mapping_title, null);
            this.setShellStyle(SWT.TITLE | SWT.MAX | SWT.RESIZE | SWT.APPLICATION_MODAL);
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
                        if (element instanceof StreamMappingAttribute) {
                            final StreamMappingAttribute attribute = (StreamMappingAttribute) element;
                            cell.setText(attribute.getMappingType().name());
                            cell.setBackground(attribute.getContainer().isComplete() ? null : UIUtils.getSharedTextColors().getColor(SharedTextColors.COLOR_WARNING));
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
                        return element instanceof StreamMappingAttribute;
                    }

                    @Override
                    protected Object getValue(Object element) {
                        return ((StreamMappingAttribute) element).getMappingType().name();
                    }

                    @Override
                    protected void setValue(Object element, Object value) {
                        ((StreamMappingAttribute) element).setMappingType(StreamMappingType.valueOf(value.toString()));
                        viewer.refresh();
                        updateCompletion();
                    }
                });
                column.getColumn().setText(DTUIMessages.stream_consumer_page_mapping_mapping_column_name);
            }

            errorLabel = new CLabel(group, SWT.NONE);
            errorLabel.setText(DTUIMessages.stream_consumer_page_mapping_label_error_no_columns_selected_text);
            errorLabel.setImage(JFaceResources.getImage(Dialog.DLG_IMG_MESSAGE_ERROR));
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
        protected void createButtonsForButtonBar(Composite parent) {
            createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
            createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        }

        @Override
        protected void okPressed() {
            final StreamConsumerSettings settings = getWizard().getPageSettings(StreamConsumerPageSettings.this, StreamConsumerSettings.class);

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

    private void refreshMappings(@NotNull DBRProgressMonitor monitor) {
        final StreamConsumerSettings settings = getWizard().getPageSettings(StreamConsumerPageSettings.this, StreamConsumerSettings.class);
        final List<DataTransferPipe> pipes = getWizard().getSettings().getDataPipes();

        mappings.clear();

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
}