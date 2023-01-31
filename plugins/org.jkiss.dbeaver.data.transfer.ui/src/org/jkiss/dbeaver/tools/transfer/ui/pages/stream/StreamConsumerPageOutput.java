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
package org.jkiss.dbeaver.tools.transfer.ui.pages.stream;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorDescriptor;
import org.jkiss.dbeaver.registry.configurator.UIPropertyConfiguratorRegistry;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferStreamWriterDescriptor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferEventProcessorConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferStreamWriterConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.pages.DataTransferPageNodeSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.HashMap;
import java.util.Map;

public class StreamConsumerPageOutput extends DataTransferPageNodeSettings {

    private static final Log log = Log.getLog(StreamConsumerPageOutput.class);

    private CTabFolder writerFolder;
    private Button showFinalMessageCheckbox;

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
        final DataTransferRegistry dataTransferRegistry = DataTransferRegistry.getInstance();
        final UIPropertyConfiguratorRegistry configuratorRegistry = UIPropertyConfiguratorRegistry.getInstance();

        writerFolder = new CTabFolder(composite, SWT.TOP | SWT.BORDER);
        writerFolder.setLayout(new GridLayout());
        writerFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        writerFolder.addSelectionListener(SelectionListener.widgetSelectedAdapter(event -> {
            final CTabItem item = writerFolder.getSelection();

            if (item.getControl() == null) {
                final var data = (StreamWriterData) item.getData();
                final var configurator = configuratorRegistry.getDescriptor(data.descriptor.getType().getImplName());

                final Composite placeholder = new Composite(writerFolder, SWT.NONE);
                placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
                placeholder.setLayout(new GridLayout());

                if (configurator == null) {
                    UIUtils
                        .createLabel(placeholder, "Nothing to configure")
                        .setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
                } else {
                    try {
                        data.configurator = configurator.createConfigurator();
                        data.configurator.createControl(placeholder, getWizard().getSettings(), this::updatePageCompletion);
                        data.configurator.loadSettings(getWizard().getSettings());
                    } catch (DBException e) {
                        UIUtils
                            .createLabel(placeholder, "Error creating configurator: " + e.getMessage())
                            .setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
                        log.error("Error creating writer configurator", e);
                    }
                }

                item.setControl(placeholder);
                composite.layout(true, true);
            }

            updatePageCompletion();
        }));

        {
            Group resultsSettings = UIUtils.createControlGroup(composite, DTUIMessages.stream_consumer_page_output_label_results, 1, GridData.FILL_HORIZONTAL, 0);

            showFinalMessageCheckbox = UIUtils.createCheckbox(resultsSettings, DTUIMessages.stream_consumer_page_output_label_show_finish_message, getWizard().getSettings().isShowFinalMessage());
            showFinalMessageCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setShowFinalMessage(showFinalMessageCheckbox.getSelection());
                }
            });

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

        setControl(composite);

    }

    private void updateControlsEnablement() {
        for (EventProcessorComposite processor : processors.values()) {
            processor.setProcessorAvailable(processor.isProcessorApplicable());
        }
    }

    @Override
    public void activatePage() {
        getWizard().loadNodeSettings();

        final DataTransferProcessorDescriptor descriptor = getWizard().getSettings().getProcessor();
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        showFinalMessageCheckbox.setSelection(getWizard().getSettings().isShowFinalMessage());

        if (descriptor.isBinaryFormat()) {
            settings.setOutputClipboard(false);
        }

        for (Map.Entry<String, EventProcessorComposite> processor : processors.entrySet()) {
            processor.getValue().setProcessorEnabled(settings.hasEventProcessor(processor.getKey()));
            processor.getValue().loadSettings(settings.getEventProcessorSettings(processor.getKey()));
        }

        for (CTabItem item : writerFolder.getItems()) {
            item.dispose();
        }

        for (var writer : DataTransferRegistry.getInstance().getStreamWriters(descriptor)) {
            final CTabItem item = new CTabItem(writerFolder, SWT.NONE);
            item.setText(writer.getLabel());
            item.setToolTipText(writer.getDescription());
            item.setImage(DBeaverIcons.getImage(writer.getIcon()));
            item.setData(new StreamWriterData(writer));

            if (settings.getWriter() != null && writer.getId().equals(settings.getWriter().getId())) {
                writerFolder.setSelection(item);
                writerFolder.notifyListeners(SWT.Selection, new Event());
            }
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

        if (writerFolder.getSelection() != null) {
            final StreamWriterData data = (StreamWriterData) writerFolder.getSelection().getData();

            if (data.configurator != null) {
                data.configurator.saveSettings(getWizard().getSettings());
            }

            settings.setWriter(data.descriptor);
        }
    }

    @Override
    protected boolean determinePageCompletion() {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);
        if (settings == null || writerFolder.getSelectionIndex() < 0) {
            return false;
        }

        final var writer = (StreamWriterData) writerFolder.getSelection().getData();
        if (writer.configurator != null && !writer.configurator.isComplete()) {
            setErrorMessage(writer.configurator.getCompletionMessage());
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

    private static class StreamWriterData {
        private final DataTransferStreamWriterDescriptor descriptor;
        private IDataTransferStreamWriterConfigurator configurator;

        public StreamWriterData(@NotNull DataTransferStreamWriterDescriptor descriptor) {
            this.descriptor = descriptor;
        }
    }
}