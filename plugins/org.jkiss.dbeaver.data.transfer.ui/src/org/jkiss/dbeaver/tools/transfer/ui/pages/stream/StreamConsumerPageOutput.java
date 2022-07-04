/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferEventProcessorConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class StreamConsumerPageOutput extends DataTransferPageNodeSettings {

    private static final Log log = Log.getLog(StreamConsumerPageOutput.class);

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
    private Button appendToEndOfFileCheck;
    private Label maximumFileSizeLabel;
    private Text maximumFileSizeText;
    private final Map<String, EventProcessorComposite> processors = new HashMap<>();

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

            directoryText = DialogUtils.createOutputFolderChooser(generalSettings, null, e -> {
                settings.setOutputFolder(directoryText.getText());
                updatePageCompletion();
            });
            ((GridData) directoryText.getParent().getLayoutData()).horizontalSpan = 4;

            UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
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

            appendToEndOfFileCheck = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_label_add_to_end_of_file, DTMessages.data_transfer_wizard_output_label_add_to_end_of_file_tip, false, 1);
            appendToEndOfFileCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setAppendToFileEnd(appendToEndOfFileCheck.getSelection());
                    updateControlsEnablement();
                }
            });

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
            Group resultsSettings = UIUtils.createControlGroup(composite, DTUIMessages.stream_consumer_page_output_label_results, 1, GridData.FILL_HORIZONTAL, 0);

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
                    final IDataTransferEventProcessorConfigurator configurator = configuratorDescriptor.createConfigurator();
                    this.processors.put(descriptor.getId(), new EventProcessorComposite(resultsSettings, settings, descriptor, configurator));
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

        setControl(composite);

    }

    private void updateControlsEnablement() {
        final DataTransferSettings settings = getWizard().getSettings();
        boolean isBinary = settings.getProcessor().isBinaryFormat();
        boolean isAppendable = settings.getProcessor().isAppendable() && !compressCheckbox.getSelection();
        boolean clipboard = !isBinary && clipboardCheck.getSelection();
        clipboardCheck.setEnabled(!isBinary);
        singleFileCheck.setEnabled(!clipboard && isAppendable && settings.getDataPipes().size() > 1 && settings.getMaxJobCount() <= 1);
        appendToEndOfFileCheck.setEnabled(!clipboard && isAppendable);
        directoryText.setEnabled(!clipboard);
        fileNameText.setEnabled(!clipboard);
        compressCheckbox.setEnabled(!clipboard && !appendToEndOfFileCheck.getSelection() && !singleFileCheck.getSelection());
        splitFilesCheckbox.setEnabled(!clipboard);
        maximumFileSizeLabel.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        maximumFileSizeText.setEnabled(!clipboard && splitFilesCheckbox.getSelection());
        encodingCombo.setEnabled(!isBinary && !clipboard);
        encodingBOMCheckbox.setEnabled(!isBinary && !clipboard);
        timestampPattern.setEnabled(!clipboard);

        for (EventProcessorComposite processor : processors.values()) {
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
        appendToEndOfFileCheck.setSelection(settings.isAppendToFileEnd() && descriptor.isAppendable());
        directoryText.setText(CommonUtils.toString(settings.getOutputFolder()));
        fileNameText.setText(CommonUtils.toString(settings.getOutputFilePattern()));
        compressCheckbox.setSelection(settings.isCompressResults());
        splitFilesCheckbox.setSelection(settings.isSplitOutFiles());
        maximumFileSizeText.setText(String.valueOf(settings.getMaxOutFileSize()));
        encodingCombo.setText(CommonUtils.toString(settings.getOutputEncoding()));
        timestampPattern.setText(settings.getOutputTimestampPattern());
        encodingBOMCheckbox.setSelection(settings.isOutputEncodingBOM() && !descriptor.isBinaryFormat());
        showFinalMessageCheckbox.setSelection(getWizard().getSettings().isShowFinalMessage());

        if (descriptor.isBinaryFormat()) {
            settings.setOutputClipboard(false);
        }

        for (Map.Entry<String, EventProcessorComposite> processor : processors.entrySet()) {
            processor.getValue().setProcessorEnabled(settings.hasEventProcessor(processor.getKey()));
            processor.getValue().loadSettings(settings.getEventProcessorSettings(processor.getKey()));
        }

        updatePageCompletion();
        updateControlsEnablement();
    }

    @Override
    public void deactivatePage() {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        for (Map.Entry<String, EventProcessorComposite> processor : processors.entrySet()) {
            final EventProcessorComposite configurator = processor.getValue();
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

        for (EventProcessorComposite processor : processors.values()) {
            if (processor.isProcessorApplicable() && processor.isProcessorEnabled() && !processor.isProcessorComplete()) {
                setErrorMessage(NLS.bind(DTMessages.data_transfer_wizard_output_event_processor_error_incomplete_configuration, processor.descriptor.getLabel()));
                return false;
            }
        }

        return true;
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

    private class EventProcessorComposite extends Composite {
        private final DataTransferEventProcessorDescriptor descriptor;
        private final IDataTransferEventProcessorConfigurator configurator;
        private final StreamConsumerSettings settings;
        private final Button enabledCheckbox;
        private Link configureLink;

        public EventProcessorComposite(@NotNull Composite parent, @NotNull StreamConsumerSettings settings, @NotNull DataTransferEventProcessorDescriptor descriptor, @Nullable IDataTransferEventProcessorConfigurator configurator) {
            super(parent, SWT.NONE);
            this.descriptor = descriptor;
            this.configurator = configurator;
            this.settings = settings;

            final boolean hasControl = configurator != null && configurator.hasControl();

            setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
            setLayout(GridLayoutFactory.fillDefaults().numColumns(hasControl ? 2 : 1).create());

            enabledCheckbox = UIUtils.createCheckbox(this, descriptor.getLabel(), descriptor.getDescription(), false, 1);
            enabledCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setProcessorEnabled(enabledCheckbox.getSelection());
                }
            });

            if (hasControl) {
                configureLink = UIUtils.createLink(this, DTMessages.data_transfer_wizard_output_event_processor_configure, new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        final ConfigureDialog dialog = new ConfigureDialog(getShell(), descriptor, configurator);
                        if (dialog.open() == IDialogConstants.OK_ID) {
                            updatePageCompletion();
                        }
                    }
                });
            }
        }

        public void loadSettings(@NotNull Map<String, Object> settings) {
            configurator.loadSettings(settings);
        }

        public void saveSettings(@NotNull Map<String, Object> settings) {
            configurator.saveSettings(settings);
        }

        public boolean isProcessorEnabled() {
            return enabledCheckbox.getEnabled() && enabledCheckbox.getSelection();
        }

        public boolean isProcessorApplicable() {
            return configurator != null && configurator.isApplicable(settings);
        }

        public boolean isProcessorComplete() {
            return configurator.isComplete();
        }

        public void setProcessorAvailable(boolean available) {
            setProcessorEnabled(enabledCheckbox.getSelection(), available);
        }

        public void setProcessorEnabled(boolean enabled) {
            setProcessorEnabled(enabled, enabledCheckbox.getEnabled());
        }

        private void setProcessorEnabled(boolean enabled, boolean available) {
            enabledCheckbox.setSelection(enabled);
            enabledCheckbox.setEnabled(available);

            if (configurator.hasControl()) {
                configureLink.setEnabled(enabled && available);
            }

            if (enabled && available) {
                settings.addEventProcessor(descriptor.getId());
            } else {
                settings.removeEventProcessor(descriptor.getId());
            }

            updatePageCompletion();
        }
    }

    private static class ConfigureDialog extends BaseDialog {
        @NotNull
        private final DataTransferEventProcessorDescriptor descriptor;
        private final IDataTransferEventProcessorConfigurator configurator;

        public ConfigureDialog(@NotNull Shell shell, @NotNull DataTransferEventProcessorDescriptor descriptor, @NotNull IDataTransferEventProcessorConfigurator configurator) {
            super(shell, NLS.bind(DTMessages.data_transfer_wizard_output_event_processor_configure_title, descriptor.getLabel()), null);
            this.descriptor = descriptor;
            this.configurator = configurator;
            setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);
            configurator.createControl(composite, descriptor, this::updateCompletion);
            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            updateCompletion();
        }

        private void updateCompletion() {
            getButton(IDialogConstants.OK_ID).setEnabled(configurator.isComplete());
        }
    }
}