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
package org.jkiss.dbeaver.tools.transfer.ui.processor.config;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.tools.transfer.processor.ExecuteCommandEventProcessor;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.ui.IDataTransferEventProcessorConfigurator;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class ExecuteCommandEventProcessorConfigurator implements IDataTransferEventProcessorConfigurator<StreamConsumerSettings> {
    private String command;
    private String workingDirectory;

    @Override
    public void createControl(@NotNull Composite parent, @NotNull StreamConsumerSettings settings, @NotNull Runnable propertyChangeListener) {
        final Composite group = new Composite(parent, SWT.NONE);
        group.setLayout(GridLayoutFactory.fillDefaults().numColumns(2).create());
        group.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).hint(300, SWT.DEFAULT).create());

        final Text commandText = UIUtils.createLabelText(group, DTUIMessages.data_transfer_event_processor_execute_command_command, command);
        commandText.addModifyListener(e -> {
            command = commandText.getText();
            propertyChangeListener.run();
        });

        UIUtils.createControlLabel(group, DTUIMessages.data_transfer_event_processor_execute_command_working_directory);
        final TextWithOpenFolder workingDirectoryText = new TextWithOpenFolder(group, DTUIMessages.data_transfer_event_processor_execute_command_working_directory_title);
        workingDirectoryText.setText(workingDirectory);
        workingDirectoryText.getTextControl().addModifyListener(e -> {
            workingDirectory = workingDirectoryText.getText();
            propertyChangeListener.run();
        });
        workingDirectoryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
    }

    @Override
    public void loadSettings(@NotNull Map<String, Object> settings) {
        command = CommonUtils.toString(settings.get(ExecuteCommandEventProcessor.PROP_COMMAND));
        workingDirectory = CommonUtils.toString(settings.get(ExecuteCommandEventProcessor.PROP_WORKING_DIRECTORY));
    }

    @Override
    public void saveSettings(@NotNull Map<String, Object> settings) {
        settings.put(ExecuteCommandEventProcessor.PROP_COMMAND, command);
        if (!CommonUtils.isEmptyTrimmed(workingDirectory)) {
            settings.put(ExecuteCommandEventProcessor.PROP_WORKING_DIRECTORY, workingDirectory);
        }
    }

    @Override
    public void resetSettings(@NotNull Map<String, Object> settings) {
    }

    @Override
    public boolean isComplete() {
        return !CommonUtils.isEmptyTrimmed(command);
    }

    @Override
    public boolean isApplicable(@NotNull StreamConsumerSettings configuration) {
        return !configuration.isOutputClipboard();
    }
}
