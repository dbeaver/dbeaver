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

import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.BlobFileConflictBehavior;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.DataFileConflictBehavior;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.LobExtractType;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferEventProcessorConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.controls.EventProcessorComposite;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.tools.transfer.ui.prefs.PrefPageDataTransfer;
import org.jkiss.dbeaver.ui.ShellUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.HelpUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StreamConsumerPageOutput extends DataTransferPageNodeSettings {
    
    private class EnumSelectionGroup<T extends Enum<T>> {
        private final Group group; 
        private final Map<T, Button> radioButtonByValue;
        private final T defaultValue;
        private final Consumer<T> onValueSelected;
        
        private T currentValue;
        
        public EnumSelectionGroup(
            @NotNull Composite parent,
            @NotNull String header,
            @NotNull List<T> values,
            @NotNull Function<T, String> titleByValue,
            @NotNull T defaultValue,
            @NotNull Consumer<T> onValueSelected,
            @NotNull Function<T, Boolean> valueSelectionConfirmation
        ) {
            group = UIUtils.createControlGroup(parent, header, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            
            SelectionListener selectionListener = SelectionListener.widgetSelectedAdapter(e -> {
                Button triggered = (Button) e.widget;
                if (triggered.getSelection()) {
                    T newValue = (T) e.widget.getData();
                    if (!currentValue.equals(newValue)) {
                        if (valueSelectionConfirmation.apply(newValue)) {
                            applyNewValue(newValue);
                        } else {
                            setValue(currentValue);
                        }
                    }
                }
            });
            
            radioButtonByValue = values.stream().collect(Collectors.toMap(
                v -> v,
                v -> UIUtils.createRadioButton(group, titleByValue.apply(v), v, selectionListener)
            ));
         
            this.defaultValue = defaultValue;
            this.currentValue = defaultValue;
            this.onValueSelected = onValueSelected;
        }
        
        @NotNull
        public T getValue() {
            return currentValue;
        }

        public T getDefaultValue() {
            return defaultValue;
        }
        
        public void setValue(@NotNull T value) {
            for (Button btn : radioButtonByValue.values()) {
                btn.setSelection(false);
            }
            
            Button valueBtn = radioButtonByValue.get(value);
            if (group.getEnabled() && !valueBtn.getEnabled()) {
                radioButtonByValue.get(defaultValue).setEnabled(true);
                applyNewValue(defaultValue);
            } else {
                valueBtn.setSelection(true);
                applyNewValue(value);
            }
        }
        
        private void applyNewValue(T newValue) {
            if (!currentValue.equals(newValue)) {
                currentValue = newValue;
                onValueSelected.accept(newValue);
            }
        }

        public void setEnabled(boolean enabled) {
            group.setEnabled(enabled);
            for (Button btn : radioButtonByValue.values()) {
                btn.setEnabled(enabled);
            }
        }

        public void setValueEnabled(T value, boolean enabled) {
            if (group.getEnabled()) {
                radioButtonByValue.get(value).setEnabled(enabled);
            }
        }
    }

    private static final Log log = Log.getLog(StreamConsumerPageOutput.class);
    private static final String HELP_DT_EXTERNAL_LINK = "Data-transfer-external-storage";
    
    private Combo encodingCombo;
    private Button encodingBOMCheckbox;
    private Text timestampPattern;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Button clipboardCheck;
    private Button singleFileCheck;
    private Button showFinalMessageCheckbox;
    private Button splitFilesCheckbox;
    private EnumSelectionGroup<DataFileConflictBehavior> dataFileConflictBehaviorSelector;
    private EnumSelectionGroup<BlobFileConflictBehavior> blobFileConflictBehaviorSelector;
    private Label maximumFileSizeLabel;
    private Text maximumFileSizeText;
    private final Map<String, EventProcessorComposite<?>> processors = new HashMap<>();

    public StreamConsumerPageOutput() {
        super(DTMessages.data_transfer_wizard_output_name);
        setTitle(DTMessages.data_transfer_wizard_output_title);
        setDescription(DTMessages.data_transfer_wizard_output_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            clipboardCheck = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_copy_to_clipboard, null, false, 5);
            clipboardCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOutputClipboard(clipboardCheck.getSelection());
                    updateControlsEnablement();
                    updatePageCompletion();
                }
            });

            // Output path/pattern

            directoryText = DialogUtils.createOutputFolderChooser(generalSettings, null, getWizard().getProject(), true, e -> {
                settings.setOutputFolder(directoryText.getText());
                updatePageCompletion();
            });
            ((GridData) directoryText.getParent().getLayoutData()).horizontalSpan = 3;

            UIUtils.createLink(generalSettings, DTMessages.data_transfer_wizard_output_label_global_settings, new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    UIUtils.showPreferencesFor(getShell(), null, PrefPageDataTransfer.PAGE_ID);
                }
            });

            UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            gd.horizontalSpan = 4;
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(e -> {
                settings.setOutputFilePattern(fileNameText.getText());
                updatePageCompletion();
            });

            {
                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_encoding);
                encodingCombo = UIUtils.createEncodingCombo(generalSettings, settings.getOutputEncoding());
                //encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingCombo.addModifyListener(e -> {
                    settings.setOutputEncoding(encodingCombo.getText());
                    updatePageCompletion();
                });
                timestampPattern = UIUtils.createLabelText(generalSettings, DTMessages.data_transfer_wizard_output_label_timestamp_pattern, GeneralUtils.DEFAULT_TIMESTAMP_PATTERN, SWT.BORDER);
                timestampPattern.addModifyListener(e -> {
                    settings.setOutputTimestampPattern(timestampPattern.getText());
                });
                encodingBOMCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_insert_bom, DTMessages.data_transfer_wizard_output_label_insert_bom_tooltip, false, 1);
                encodingBOMCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setOutputEncodingBOM(encodingBOMCheckbox.getSelection());
                    }
                });
            }
            
            singleFileCheck = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_use_single_file, DTMessages.data_transfer_wizard_output_label_use_single_file_tip, false, 5);
            singleFileCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setUseSingleFile(singleFileCheck.getSelection());
                    updateControlsEnablement();
                }
            });
            compressCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_compress, null, false, 1);
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setCompressResults(compressCheckbox.getSelection());
                    updateControlsEnablement();
                }
            });

            {
                Composite outFilesSettings = UIUtils.createComposite(generalSettings, 3);
                outFilesSettings.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, true, false, 5, 1));

                splitFilesCheckbox = UIUtils.createCheckbox(outFilesSettings, DTMessages.data_transfer_wizard_output_checkbox_split_files, DTMessages.data_transfer_wizard_output_checkbox_split_files_tip, false, 1);
                splitFilesCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setSplitOutFiles(splitFilesCheckbox.getSelection());
                        updateControlsEnablement();
                    }
                });
                maximumFileSizeLabel = UIUtils.createControlLabel(outFilesSettings, DTUIMessages.stream_consumer_page_output_label_maximum_file_size);
                maximumFileSizeText = new Text(outFilesSettings, SWT.BORDER);
                maximumFileSizeText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
                maximumFileSizeText.addModifyListener(e ->
                    settings.setMaxOutFileSize(CommonUtils.toLong(maximumFileSizeText.getText())));
                gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = UIUtils.getFontHeight(maximumFileSizeText) * 10;
                maximumFileSizeText.setLayoutData(gd);
            }

            {
                final ExpandableComposite expander = new ExpandableComposite(generalSettings, SWT.NONE);
                expander.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 5, 1));
                expander.addExpansionListener(new ExpansionAdapter() {
                    @Override
                    public void expansionStateChanged(ExpansionEvent e) {
                        updateFileConflictExpanderTitle(expander, settings);
                        UIUtils.resizeShell(parent.getShell());
                    }
                });
                Composite fileConflictBehaviorSettings = UIUtils.createComposite(expander, 2);
                expander.setClient(fileConflictBehaviorSettings);
                updateFileConflictExpanderTitle(expander, settings);
                
                dataFileConflictBehaviorSelector = new EnumSelectionGroup<>(
                    fileConflictBehaviorSettings,
                    DTMessages.data_transfer_file_conflict_behavior_setting,
                    List.of(
                        DataFileConflictBehavior.ASK,
                        DataFileConflictBehavior.APPEND,
                        DataFileConflictBehavior.PATCHNAME,
                        DataFileConflictBehavior.OVERWRITE
                    ),
                    v -> v.title,
                    DataFileConflictBehavior.ASK,
                    v -> {
                        settings.setDataFileConflictBehavior(v);
                        updateFileConflictExpanderTitle(expander, settings);
                    },
                    v -> v != DataFileConflictBehavior.OVERWRITE || confirmPossibleFileOverwrite()
                );
                blobFileConflictBehaviorSelector = new EnumSelectionGroup<>(
                    fileConflictBehaviorSettings,
                    DTMessages.data_transfer_blob_file_conflict_behavior_setting,
                    List.of(BlobFileConflictBehavior.ASK, BlobFileConflictBehavior.PATCHNAME, BlobFileConflictBehavior.OVERWRITE),
                    v -> v.title,
                    BlobFileConflictBehavior.ASK,
                    v -> {
                        settings.setBlobFileConflictBehavior(v);
                        updateFileConflictExpanderTitle(expander, settings);
                    },
                    v -> v != BlobFileConflictBehavior.OVERWRITE || confirmPossibleFileOverwrite()
                );
            }

            // No resolver - several producers may present.
            new VariablesHintLabel(
                generalSettings,
                DTUIMessages.stream_consumer_page_output_variables_hint_label,
                DTUIMessages.stream_consumer_page_output_variables_hint_label,
                StreamTransferConsumer.VARIABLES,
                true
            );
        }

        {
            Group resultsSettings = UIUtils.createControlGroup(composite, DTUIMessages.stream_consumer_page_output_label_results, 2, GridData.FILL_HORIZONTAL, 0);

            showFinalMessageCheckbox = UIUtils.createCheckbox(resultsSettings, DTUIMessages.stream_consumer_page_output_label_show_finish_message, getWizard().getSettings().isShowFinalMessage());
            showFinalMessageCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setShowFinalMessage(showFinalMessageCheckbox.getSelection());
                }
            });

            final DataTransferRegistry dataTransferRegistry = DataTransferRegistry.getInstance();
            final UIPropertyConfiguratorRegistry configuratorRegistry = UIPropertyConfiguratorRegistry.getInstance();

            for (DataTransferEventProcessorDescriptor descriptor : dataTransferRegistry.getEventProcessors(StreamTransferConsumer.NODE_ID)) {
                try {
                    final UIPropertyConfiguratorDescriptor configuratorDescriptor = configuratorRegistry.getDescriptor(descriptor.getType().getImplName());
                    final IDataTransferEventProcessorConfigurator<StreamConsumerSettings> configurator = configuratorDescriptor.createConfigurator();
                    this.processors.put(descriptor.getId(), new EventProcessorComposite<>(this::updatePageCompletion, resultsSettings, settings, descriptor, configurator));
                } catch (Exception e) {
                    log.error("Can't create event processor", e);
                }
            }
        }

        {
            final String[] variables = getAvailableVariables();
            final StringContentProposalProvider proposalProvider = new StringContentProposalProvider(Arrays
                .stream(variables)
                .map(GeneralUtils::variablePattern)
                .toArray(String[]::new));

            UIUtils.setContentProposalToolTip(directoryText, DTUIMessages.stream_consumer_page_output_tooltip_output_directory_pattern, variables);
            UIUtils.setContentProposalToolTip(fileNameText, DTUIMessages.stream_consumer_page_output_tooltip_output_file_name_pattern, variables);

            ContentAssistUtils.installContentProposal(directoryText, new SmartTextContentAdapter(), proposalProvider);
            ContentAssistUtils.installContentProposal(fileNameText, new SmartTextContentAdapter(), proposalProvider);
        }

        UIUtils.createLink(
            composite,
            DTMessages.data_transfer_wizard_output_export_to_external_storage_link,
            SelectionListener.widgetSelectedAdapter(
                e -> ShellUtils.launchProgram(HelpUtils.getHelpExternalReference(HELP_DT_EXTERNAL_LINK)))
        );

        setControl(composite);

    }

    private void updateFileConflictExpanderTitle(ExpandableComposite expander, StreamConsumerSettings settings) {
        if (expander.isExpanded()) {
            expander.setText(DTMessages.data_transfer_file_name_conflict_behavior_setting_text);
        } else {
            String text = DTMessages.data_transfer_file_conflict_behavior_setting + ": " + settings.getDataFileConflictBehavior().title +
                "; " + DTMessages.data_transfer_blob_file_conflict_behavior_setting + ": " + settings.getBlobFileConflictBehavior().title;
            expander.setText(text);
        }
    }

    private void updateControlsEnablement() {
        final DataTransferSettings settings = getWizard().getSettings();
        boolean isBinary = settings.getProcessor().isBinaryFormat();
        boolean isAppendable = settings.getProcessor().isAppendable() && !compressCheckbox.getSelection();
        boolean clipboard = !isBinary && clipboardCheck.getSelection();
        clipboardCheck.setEnabled(!isBinary);
        singleFileCheck.setEnabled(!clipboard && isAppendable && settings.getDataPipes().size() > 1 && settings.getMaxJobCount() <= 1);
        dataFileConflictBehaviorSelector.setEnabled(!clipboard);
        dataFileConflictBehaviorSelector.setValueEnabled(DataFileConflictBehavior.APPEND, isAppendable);
        blobFileConflictBehaviorSelector.setEnabled(
            !clipboard && getWizard().getPageSettings(this, StreamConsumerSettings.class).getLobExtractType() == LobExtractType.FILES
        );
        directoryText.setEnabled(!clipboard);
        fileNameText.setEnabled(!clipboard);
        compressCheckbox.setEnabled(!clipboard && !singleFileCheck.getSelection());
        if (compressCheckbox.getSelection() && dataFileConflictBehaviorSelector.getValue().equals(DataFileConflictBehavior.APPEND)) {
            dataFileConflictBehaviorSelector.setValue(DataFileConflictBehavior.PATCHNAME);
        }
        splitFilesCheckbox.setEnabled(!clipboard);
        maximumFileSizeLabel.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        maximumFileSizeText.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        encodingCombo.setEnabled(!isBinary && !clipboard);
        encodingBOMCheckbox.setEnabled(!isBinary && !clipboard);
        timestampPattern.setEnabled(!clipboard);

        for (EventProcessorComposite<?> processor : processors.values()) {
            processor.setProcessorAvailable(processor.isProcessorApplicable());
        }
    }

    @Override
    public void activatePage() {
        getWizard().loadNodeSettings();

        final DataTransferProcessorDescriptor descriptor = getWizard().getSettings().getProcessor();
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        clipboardCheck.setSelection(settings.isOutputClipboard() && !descriptor.isBinaryFormat());
        singleFileCheck.setSelection(settings.isUseSingleFile() && descriptor.isAppendable());
        dataFileConflictBehaviorSelector.setValue(settings.getDataFileConflictBehavior());
        blobFileConflictBehaviorSelector.setValue(settings.getBlobFileConflictBehavior());
        directoryText.setText(CommonUtils.toString(settings.getOutputFolder()));
        fileNameText.setText(CommonUtils.toString(settings.getOutputFilePattern()));
        compressCheckbox.setSelection(settings.isCompressResults());
        splitFilesCheckbox.setSelection(settings.isSplitOutFiles());
        maximumFileSizeText.setText(String.valueOf(settings.getMaxOutFileSize()));
        encodingCombo.setText(CommonUtils.toString(settings.getOutputEncoding()));
        timestampPattern.setText(settings.getOutputTimestampPattern());
        encodingBOMCheckbox.setSelection(settings.isOutputEncodingBOM() && !descriptor.isBinaryFormat());
        showFinalMessageCheckbox.setSelection(getWizard().getSettings().isShowFinalMessage());
        
        if (!getWizard().getSettings().getProcessor().isAppendable() || settings.isCompressResults()) {
            if (settings.getDataFileConflictBehavior() == DataFileConflictBehavior.APPEND) {
                dataFileConflictBehaviorSelector.setValue(dataFileConflictBehaviorSelector.getDefaultValue());
            }
        }

        if (descriptor.isBinaryFormat()) {
            settings.setOutputClipboard(false);
        }

        for (Map.Entry<String, EventProcessorComposite<?>> processor : processors.entrySet()) {
            processor.getValue().setProcessorEnabled(settings.hasEventProcessor(processor.getKey()));
            processor.getValue().loadSettings(settings.getEventProcessorSettings(processor.getKey()));
        }

        updatePageCompletion();
        updateControlsEnablement();
    }

    @Override
    public void deactivatePage() {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        for (Map.Entry<String, EventProcessorComposite<?>> processor : processors.entrySet()) {
            final EventProcessorComposite<?> configurator = processor.getValue();
            if (configurator.isProcessorEnabled() && configurator.isProcessorApplicable() && configurator.isProcessorComplete()) {
                configurator.saveSettings(settings.getEventProcessorSettings(processor.getKey()));
            }
        }
    }

    @Override
    protected boolean determinePageCompletion() {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        if (settings == null) {
            return false;
        }
        if (settings.isOutputClipboard()) {
            return true;
        }

        if (CommonUtils.isEmpty(settings.getOutputFolder())) {
            setErrorMessage(DTMessages.data_transfer_wizard_output_error_empty_output_directory);
            return false;
        }
        if (CommonUtils.isEmpty(settings.getOutputFilePattern())) {
            setErrorMessage(DTMessages.data_transfer_wizard_output_error_empty_output_filename);
            return false;
        }
        try {
            Charset.forName(settings.getOutputEncoding());
        } catch (Exception e) {
            setErrorMessage(DTMessages.data_transfer_wizard_output_error_invalid_charset);
            return false;
        }

        for (EventProcessorComposite<?> processor : processors.values()) {
            if (processor.isProcessorApplicable() && processor.isProcessorEnabled() && !processor.isProcessorComplete()) {
                setErrorMessage(NLS.bind(DTMessages.data_transfer_wizard_output_event_processor_error_incomplete_configuration, processor.getDescriptor().getLabel()));
                return false;
            }
        }
        return true;
    }
    
    private boolean confirmPossibleFileOverwrite() {
        return DBWorkbench.getPlatformUI().confirmAction(
            DTMessages.data_transfer_file_conflict_confirm_override_title, 
            DTMessages.data_transfer_file_conflict_confirm_override_message,
            true
        );
    }

    @NotNull
    private String[] getAvailableVariables() {
        final Set<String> variables = Arrays.stream(StreamTransferConsumer.VARIABLES)
            .map(x -> x[0])
            .collect(Collectors.toCollection(LinkedHashSet::new));

        final List<DataTransferPipe> pipes = getWizard().getSettings().getDataPipes();
        if (pipes.size() == 1) {
            final DBSObject object = pipes.get(0).getProducer().getDatabaseObject();
            final SQLQueryContainer container = DBUtils.getAdapter(SQLQueryContainer.class, object);
            if (container != null) {
                variables.addAll(container.getQueryParameters().keySet());
            }
        }

        return variables.toArray(new String[0]);
    }

    @Override
    public boolean isPageApplicable() {
        return isConsumerOfType(StreamTransferConsumer.class);
    }
}