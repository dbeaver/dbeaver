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
package org.jkiss.dbeaver.tools.transfer.ui.controls;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferEventProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferEventProcessorConfigurator;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;

import java.util.Map;

public class EventProcessorComposite<T extends IDataTransferConsumerSettings> extends Composite {
    private final Runnable propertyChangeListener;
    private final DataTransferEventProcessorDescriptor descriptor;
    private final IDataTransferEventProcessorConfigurator<T> configurator;
    private final T settings;
    private final Button enabledCheckbox;
    private Link configureLink;
    private Map<String, Object> rawSettings;

    public EventProcessorComposite(@NotNull Runnable propertyChangeListener, @NotNull Composite parent, @NotNull T settings, @NotNull DataTransferEventProcessorDescriptor descriptor, @Nullable IDataTransferEventProcessorConfigurator<T> configurator) {
        super(parent, SWT.NONE);
        this.propertyChangeListener = propertyChangeListener;
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
                    if (rawSettings != null) {
                        configurator.loadSettings(rawSettings);
                        rawSettings = null;
                    }
                    final ConfigureDialog dialog = new ConfigureDialog(getShell());
                    if (dialog.open() == IDialogConstants.OK_ID) {
                        propertyChangeListener.run();
                    }
                }
            });
        }
    }

    public void loadSettings(@NotNull Map<String, Object> settings) {
        if (isProcessorEnabled()) {
            configurator.loadSettings(settings);
        } else {
            rawSettings = settings;
        }
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

    @NotNull
    public DataTransferEventProcessorDescriptor getDescriptor() {
        return descriptor;
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
            settings.addEventProcessor(descriptor);
        } else {
            settings.removeEventProcessor(descriptor);
        }

        propertyChangeListener.run();
    }

    private class ConfigureDialog extends BaseDialog {
        public ConfigureDialog(@NotNull Shell shell) {
            super(shell, NLS.bind(DTMessages.data_transfer_wizard_output_event_processor_configure_title, descriptor.getLabel()), null);
            setShellStyle(SWT.DIALOG_TRIM | SWT.RESIZE | SWT.APPLICATION_MODAL);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            final Composite composite = super.createDialogArea(parent);
            configurator.createControl(composite, settings, this::updateCompletion);
            return composite;
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent) {
            super.createButtonsForButtonBar(parent);
            updateCompletion();
        }

        private void updateCompletion() {
            final Button button = getButton(IDialogConstants.OK_ID);
            if (button != null) {
                button.setEnabled(configurator.isComplete());
            }
        }
    }
}
