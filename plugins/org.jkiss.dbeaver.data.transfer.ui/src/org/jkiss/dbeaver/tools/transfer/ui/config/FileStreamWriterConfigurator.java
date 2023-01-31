/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.config;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLQueryContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.BlobFileConflictBehavior;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings.DataFileConflictBehavior;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferStreamWriterConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.charset.Charset;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FileStreamWriterConfigurator implements IDataTransferStreamWriterConfigurator {
    private Text directoryText;
    private Text filenamePatternText;
    private Text timestampPatternText;
    private Combo encodingCombo;
    private Button insertBomCheck;
    private Button singleOutputFileCheck;
    private Button compressOutputFileCheck;
    private Button splitOutputFileCheck;
    private Spinner splitOutputFileSizeSpinner;

    private ExpandableComposite fileConflictComposite;
    private EnumSelectionGroup<DataFileConflictBehavior> dataFileConflictBehaviorSelector;
    private EnumSelectionGroup<BlobFileConflictBehavior> blobFileConflictBehaviorSelector;

    @Override
    public void createControl(@NotNull Composite parent, @NotNull DataTransferSettings transferSettings, @NotNull Runnable listener) {
        final Group general = UIUtils.createControlGroup(parent, "General", 4, GridData.FILL_HORIZONTAL, 0);
        final ModifyListener modifyListener = e -> listener.run();

        directoryText = DialogUtils.createOutputFolderChooser(general, null, modifyListener);
        ((GridData) directoryText.getParent().getLayoutData()).horizontalSpan = 3;

        filenamePatternText = UIUtils.createLabelText(general, DTMessages.data_transfer_wizard_output_label_file_name_pattern, "");
        filenamePatternText.addModifyListener(modifyListener);

        timestampPatternText = UIUtils.createLabelText(general, DTMessages.data_transfer_wizard_output_label_timestamp_pattern, "");
        timestampPatternText.addModifyListener(modifyListener);

        UIUtils.createControlLabel(general, DTMessages.data_transfer_wizard_output_label_encoding);
        encodingCombo = UIUtils.createEncodingCombo(general, null);
        encodingCombo.addModifyListener(modifyListener);

        insertBomCheck = UIUtils.createCheckbox(
            general,
            DTMessages.data_transfer_wizard_output_label_insert_bom,
            DTMessages.data_transfer_wizard_output_label_insert_bom_tooltip,
            false,
            2
        );

        final Group options = UIUtils.createControlGroup(parent, "Options", 1, GridData.FILL_HORIZONTAL, 0);
        options.setLayoutData(GridDataFactory.swtDefaults().align(SWT.FILL, SWT.BEGINNING).span(4, 1).create());

        singleOutputFileCheck = UIUtils.createCheckbox(options, DTMessages.data_transfer_wizard_output_label_use_single_file, false);
        compressOutputFileCheck = UIUtils.createCheckbox(options, DTMessages.data_transfer_wizard_output_checkbox_compress, false);

        final Composite placeholder = UIUtils.createPlaceholder(options, 3);

        splitOutputFileCheck = UIUtils.createCheckbox(
            placeholder,
            DTMessages.data_transfer_wizard_output_checkbox_split_files,
            DTMessages.data_transfer_wizard_output_checkbox_split_files_tip,
            false,
            1
        );
        splitOutputFileCheck.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> updateControlEnablement(transferSettings)));

        splitOutputFileSizeSpinner = UIUtils.createLabelSpinner(
            placeholder,
            DTMessages.data_transfer_wizard_output_label_segment_size,
            0,
            100000,
            Integer.MAX_VALUE
        );

        fileConflictComposite = new ExpandableComposite(general, SWT.NONE);
        fileConflictComposite.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 5, 1));
        fileConflictComposite.addExpansionListener(new ExpansionAdapter() {
            @Override
            public void expansionStateChanged(ExpansionEvent e) {
                updateControlEnablement(transferSettings);
                UIUtils.resizeShell(parent.getShell());
            }
        });
        Composite fileConflictBehaviorSettings = UIUtils.createComposite(fileConflictComposite, 2);
        fileConflictComposite.setClient(fileConflictBehaviorSettings);

        dataFileConflictBehaviorSelector = new EnumSelectionGroup<>(
            fileConflictBehaviorSettings,
            DTMessages.data_transfer_file_conflict_behavior_setting,
            List.of(
                DataFileConflictBehavior.ASK,
                DataFileConflictBehavior.APPEND,
                DataFileConflictBehavior.PATCHNAME,
                DataFileConflictBehavior.OVERWRITE
            ),
            DataFileConflictBehavior::getTitle,
            DataFileConflictBehavior.ASK,
            v -> {
                final StreamConsumerSettings settings = getConsumerSettings(transferSettings);
                settings.setDataFileConflictBehavior(v);
                updateControlEnablement(transferSettings);
            },
            v -> v != DataFileConflictBehavior.OVERWRITE || confirmPossibleFileOverwrite()
        );
        blobFileConflictBehaviorSelector = new EnumSelectionGroup<>(
            fileConflictBehaviorSettings,
            DTMessages.data_transfer_blob_file_conflict_behavior_setting,
            List.of(BlobFileConflictBehavior.ASK, BlobFileConflictBehavior.PATCHNAME, BlobFileConflictBehavior.OVERWRITE),
            BlobFileConflictBehavior::getTitle,
            BlobFileConflictBehavior.ASK,
            v -> {
                final StreamConsumerSettings settings = getConsumerSettings(transferSettings);
                settings.setBlobFileConflictBehavior(v);
                updateControlEnablement(transferSettings);
            },
            v -> v != BlobFileConflictBehavior.OVERWRITE || confirmPossibleFileOverwrite()
        );

        {
            final String[] variables = getAvailableVariables(transferSettings);
            final StringContentProposalProvider proposalProvider = new StringContentProposalProvider(Arrays
                .stream(variables)
                .map(GeneralUtils::variablePattern)
                .toArray(String[]::new));

            UIUtils.setContentProposalToolTip(
                directoryText, DTUIMessages.stream_consumer_page_output_tooltip_output_directory_pattern, variables);
            UIUtils.setContentProposalToolTip(
                filenamePatternText, DTUIMessages.stream_consumer_page_output_tooltip_output_file_name_pattern, variables);

            ContentAssistUtils.installContentProposal(directoryText, new SmartTextContentAdapter(), proposalProvider);
            ContentAssistUtils.installContentProposal(filenamePatternText, new SmartTextContentAdapter(), proposalProvider);
        }

        // No resolver - several producers may present.
        new VariablesHintLabel(
            general,
            DTUIMessages.stream_consumer_page_output_variables_hint_label,
            DTUIMessages.stream_consumer_page_output_variables_hint_label,
            StreamTransferConsumer.VARIABLES,
            true
        );
    }

    @NotNull
    private StreamConsumerSettings getConsumerSettings(DataTransferSettings transferSettings) {
        return (StreamConsumerSettings) Objects.requireNonNull(transferSettings.getNodeSettings(transferSettings.getConsumer()));
    }

    @Override
    public void loadSettings(@NotNull DataTransferSettings transferSettings) {
        final StreamConsumerSettings settings = getConsumerSettings(transferSettings);

        directoryText.setText(settings.getOutputFolder());
        filenamePatternText.setText(settings.getOutputFilePattern());
        timestampPatternText.setText(settings.getOutputTimestampPattern());
        encodingCombo.setText(settings.getOutputEncoding());
        insertBomCheck.setSelection(settings.isOutputEncodingBOM());
        singleOutputFileCheck.setSelection(settings.isUseSingleFile());
        compressOutputFileCheck.setSelection(settings.isCompressResults());
        splitOutputFileCheck.setSelection(settings.isSplitOutFiles());
        splitOutputFileCheck.notifyListeners(SWT.Selection, new Event());
        splitOutputFileSizeSpinner.setSelection((int) settings.getMaxOutFileSize());
        dataFileConflictBehaviorSelector.setValue(settings.getDataFileConflictBehavior());
        blobFileConflictBehaviorSelector.setValue(settings.getBlobFileConflictBehavior());

        if (!transferSettings.getProcessor().isAppendable() || settings.isCompressResults()) {
            if (settings.getDataFileConflictBehavior() == DataFileConflictBehavior.APPEND) {
                dataFileConflictBehaviorSelector.setValue(dataFileConflictBehaviorSelector.getDefaultValue());
            }
        }

        updateControlEnablement(transferSettings);
    }

    @Override
    public void saveSettings(@NotNull DataTransferSettings transferSettings) {
        final StreamConsumerSettings settings = getConsumerSettings(transferSettings);

        settings.setOutputFolder(directoryText.getText());
        settings.setOutputFilePattern(filenamePatternText.getText());
        settings.setOutputTimestampPattern(timestampPatternText.getText());
        settings.setOutputEncoding(encodingCombo.getText());
        settings.setOutputEncodingBOM(insertBomCheck.getSelection());
        settings.setUseSingleFile(singleOutputFileCheck.getSelection());
        settings.setCompressResults(compressOutputFileCheck.getSelection());
        settings.setSplitOutFiles(splitOutputFileCheck.getSelection());
        settings.setMaxOutFileSize(splitOutputFileSizeSpinner.getSelection());
        settings.setDataFileConflictBehavior(dataFileConflictBehaviorSelector.getValue());
        settings.setBlobFileConflictBehavior(blobFileConflictBehaviorSelector.getValue());
    }

    @Nullable
    @Override
    public String getCompletionMessage() {
        if (CommonUtils.isEmptyTrimmed(directoryText.getText())) {
            return DTMessages.data_transfer_wizard_output_error_empty_output_directory;
        }
        if (CommonUtils.isEmptyTrimmed(filenamePatternText.getText())) {
            return DTMessages.data_transfer_wizard_output_error_empty_output_filename;
        }
        try {
            Charset.forName(encodingCombo.getText());
        } catch (Exception e) {
            return DTMessages.data_transfer_wizard_output_error_invalid_charset;
        }
        return null;
    }

    private void updateControlEnablement(@NotNull DataTransferSettings transferSettings) {
        final StreamConsumerSettings settings = getConsumerSettings(transferSettings);
        final boolean isBinary = transferSettings.getProcessor().isBinaryFormat();
        final boolean isAppendable = transferSettings.getProcessor().isAppendable() && !compressOutputFileCheck.getSelection();

        encodingCombo.setEnabled(!isBinary);
        insertBomCheck.setEnabled(!isBinary);
        splitOutputFileSizeSpinner.setEnabled(splitOutputFileCheck.getSelection());

        dataFileConflictBehaviorSelector.setValueEnabled(DataFileConflictBehavior.APPEND, isAppendable);
        blobFileConflictBehaviorSelector.setEnabled(settings.getLobExtractType() == StreamConsumerSettings.LobExtractType.FILES);

        singleOutputFileCheck.setEnabled(isAppendable
            && transferSettings.getDataPipes().size() > 1
            && transferSettings.getMaxJobCount() <= 1);

        compressOutputFileCheck.setEnabled(dataFileConflictBehaviorSelector.getValue() != DataFileConflictBehavior.APPEND
            && dataFileConflictBehaviorSelector.getValue() != DataFileConflictBehavior.ASK
            && blobFileConflictBehaviorSelector.getValue() != BlobFileConflictBehavior.ASK && !singleOutputFileCheck.getSelection());

        updateFileConflictExpanderTitle(fileConflictComposite, settings);
    }

    private void updateFileConflictExpanderTitle(ExpandableComposite expander, StreamConsumerSettings settings) {
        if (expander.isExpanded()) {
            expander.setText("File name conflict behavior settings");
        } else {
            expander.setText(String.format(
                "%s: %s; %s: %s",
                DTMessages.data_transfer_file_conflict_behavior_setting, settings.getDataFileConflictBehavior().getTitle(),
                DTMessages.data_transfer_blob_file_conflict_behavior_setting, settings.getBlobFileConflictBehavior().getTitle()
            ));
        }
    }

    private boolean confirmPossibleFileOverwrite() {
        return DBWorkbench.getPlatformUI().confirmAction(
            DTMessages.data_transfer_file_conflict_confirm_override_title,
            DTMessages.data_transfer_file_conflict_confirm_override_message,
            true
        );
    }

    @NotNull
    private static String[] getAvailableVariables(@NotNull DataTransferSettings settings) {
        final Set<String> variables = Arrays.stream(StreamTransferConsumer.VARIABLES)
            .map(x -> x[0])
            .collect(Collectors.toCollection(LinkedHashSet::new));

        final List<DataTransferPipe> pipes = settings.getDataPipes();

        if (pipes.size() == 1) {
            final DBSObject object = pipes.get(0).getProducer().getDatabaseObject();
            final SQLQueryContainer container = DBUtils.getAdapter(SQLQueryContainer.class, object);

            if (container != null) {
                variables.addAll(container.getQueryParameters().keySet());
            }
        }

        return variables.toArray(new String[0]);
    }

    private static class EnumSelectionGroup<T extends Enum<T>> {
        private final Group group;
        private final Map<T, Button> radioButtonByValue;
        private final T defaultValue;
        private final Consumer<T> onValueSelected;

        private T currentValue;

        @SuppressWarnings("unchecked")
        public EnumSelectionGroup(
            @NotNull Composite parent,
            @NotNull String header,
            @NotNull List<T> values,
            @NotNull Function<T, String> titleByValue,
            @NotNull T defaultValue,
            @NotNull Consumer<T> onValueSelected,
            @NotNull Function<T, Boolean> valueSelectionConfirmation
        ) {
            final SelectionListener listener = SelectionListener.widgetSelectedAdapter(e -> {
                if (((Button) e.widget).getSelection()) {
                    final T newValue = (T) e.widget.getData();
                    if (!currentValue.equals(newValue)) {
                        if (valueSelectionConfirmation.apply(newValue)) {
                            applyNewValue(newValue);
                        } else {
                            setValue(currentValue);
                        }
                    }
                }
            });

            group = UIUtils.createControlGroup(parent, header, 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
            radioButtonByValue = values.stream().collect(Collectors.toMap(
                Function.identity(),
                v -> UIUtils.createRadioButton(group, titleByValue.apply(v), v, listener)
            ));

            this.defaultValue = defaultValue;
            this.currentValue = defaultValue;
            this.onValueSelected = onValueSelected;
        }

        @NotNull
        public T getValue() {
            return currentValue;
        }

        @NotNull
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

        private void applyNewValue(@NotNull T newValue) {
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

        public void setValueEnabled(@NotNull T value, boolean enabled) {
            if (group.getEnabled()) {
                radioButtonByValue.get(value).setEnabled(enabled);
            }
        }
    }
}
