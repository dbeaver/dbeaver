/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TextWithOpenFolder;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Events edit dialog page
 */
public class ConnectionPageShellCommands extends ConnectionWizardPage {

    public static final String PAGE_NAME = ConnectionPageShellCommands.class.getSimpleName();

    private static final String CoreMessagesdialog_connection_edit_wizard_shell_cmd_directory_title = null;
    private DataSourceDescriptor dataSource;
    private Text commandText;
    private Button showProcessCheck;
    private Button waitFinishCheck;
    private Spinner waitFinishTimeoutMs;
    private Button terminateCheck;
    private Spinner pauseAfterExecute;
    private TextWithOpenFolder workingDirectory;

    private Table eventTypeTable;

    private final Map<DBPConnectionEventType, DBRShellCommand> eventsCache = new HashMap<>();

    protected ConnectionPageShellCommands(DataSourceDescriptor dataSource)
    {
        super(PAGE_NAME);
        this.dataSource = dataSource;
        setTitle(CoreMessages.dialog_connection_edit_wizard_shell_cmd);
        setDescription(CoreMessages.dialog_connection_events_title);
        setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.EVENT));
        for (DBPConnectionEventType eventType : DBPConnectionEventType.values()) {
            DBRShellCommand command = dataSource.getConnectionConfiguration().getEvent(eventType);
            eventsCache.put(eventType, command == null ? null : new DBRShellCommand(command));
        }
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 2, 5);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite eventGroup = UIUtils.createPlaceholder(group, 1);
            eventGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            UIUtils.createControlLabel(eventGroup, CoreMessages.dialog_connection_events_label_event);
            eventTypeTable = new Table(eventGroup, SWT.BORDER | SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION);
            eventTypeTable.setLayoutData(new GridData(GridData.FILL_VERTICAL));
            eventTypeTable.addListener(SWT.Selection, event -> {
                if (event.detail == SWT.CHECK) {
                    eventTypeTable.select(eventTypeTable.indexOf((TableItem) event.item));
                }
            });

            for (DBPConnectionEventType eventType : DBPConnectionEventType.values()) {
                DBRShellCommand command = eventsCache.get(eventType);
                TableItem item = new TableItem(eventTypeTable, SWT.NONE);
                item.setData(eventType);
                item.setText(eventType.getTitle());
                item.setImage(DBeaverIcons.getImage(UIIcon.EVENT));
                item.setChecked(command != null && command.isEnabled());
            }

            eventTypeTable.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    DBPConnectionEventType eventType = getSelectedEventType();
                    selectEventType(eventType);
                    DBRShellCommand command = eventType == null ? null : eventsCache.get(eventType);
                    boolean enabled = ((TableItem) e.item).getChecked();
                    if (enabled || (command != null && enabled != command.isEnabled())) {
                        updateEvent(false);
                    }
                }
            });
        }
        {
            Composite detailsGroup = UIUtils.createPlaceholder(group, 1, 5);
            detailsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));

            UIUtils.createControlLabel(detailsGroup, CoreMessages.dialog_connection_events_label_command);
            commandText = new Text(detailsGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            commandText.addModifyListener(e -> updateEvent(true));
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.heightHint = 60;
            gd.widthHint = 300;
            commandText.setLayoutData(gd);

            SelectionAdapter eventEditAdapter = new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    updateEvent(false);
                }
            };

            showProcessCheck = UIUtils.createCheckbox(detailsGroup, CoreMessages.dialog_connection_events_checkbox_show_process, false);
            showProcessCheck.addSelectionListener(eventEditAdapter);
            waitFinishCheck = UIUtils.createCheckbox(detailsGroup, CoreMessages.dialog_connection_events_checkbox_wait_finish, false);
            waitFinishCheck.addSelectionListener(eventEditAdapter);
            waitFinishTimeoutMs = createWaitFinishTimeout(detailsGroup);
            waitFinishTimeoutMs.addSelectionListener(eventEditAdapter);
            terminateCheck = UIUtils.createCheckbox(detailsGroup, CoreMessages.dialog_connection_events_checkbox_terminate_at_disconnect, false);
            terminateCheck.addSelectionListener(eventEditAdapter);
            {
                Composite pauseComposite = UIUtils.createPlaceholder(detailsGroup, 2, 5);
                pauseComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

                pauseAfterExecute = UIUtils.createLabelSpinner(pauseComposite, CoreMessages.dialog_connection_edit_wizard_shell_cmd_pause_label, CoreMessages.dialog_connection_edit_wizard_shell_cmd_pause_tooltip, 0, 0, Integer.MAX_VALUE);
                pauseAfterExecute.addSelectionListener(eventEditAdapter);

                UIUtils.createControlLabel(pauseComposite, CoreMessages.dialog_connection_edit_wizard_shell_cmd_directory_label);
                workingDirectory = new TextWithOpenFolder(pauseComposite, CoreMessagesdialog_connection_edit_wizard_shell_cmd_directory_title);
                workingDirectory.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                workingDirectory.getTextControl().addModifyListener(e -> {
                    DBRShellCommand command = getActiveCommand();
                    if (command != null) {
                        command.setWorkingDirectory(workingDirectory.getText());
                    }
                });
            }

            VariablesHintLabel variablesHintLabel = new VariablesHintLabel(
                detailsGroup,
                CoreMessages.dialog_connection_edit_wizard_shell_cmd_variables_hint_label,
                CoreMessages.dialog_connection_edit_wizard_shell_cmd_variables_hint_title,
                DataSourceDescriptor.CONNECT_VARIABLES);
            variablesHintLabel.setResolver(new DataSourceVariableResolver(dataSource, dataSource.getConnectionConfiguration()));
        }

        selectEventType(null);

        setControl(group);
    }

    private static Spinner createWaitFinishTimeout(Composite detailsGroup) {
        Composite waitFinishGroup = new Composite(detailsGroup, SWT.NONE);
        GridLayout waitFinishGroupLayout = new GridLayout(2, false);
        waitFinishGroupLayout.marginWidth = 0;
        waitFinishGroupLayout.marginHeight = 0;
        waitFinishGroupLayout.marginLeft = 25;
        waitFinishGroup.setLayout(waitFinishGroupLayout);

        GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
        waitFinishGroup.setLayoutData(gridData);

        int defaultValue = DBRShellCommand.WAIT_PROCESS_TIMEOUT_FOREVER;
        int maxSelection = DBRShellCommand.WAIT_PROCESS_TIMEOUT_MAX_SELECTION;
        Spinner spinner = UIUtils.createSpinner(waitFinishGroup, "-1 to wait forever", 0, defaultValue, maxSelection);
        UIUtils.createLabel(waitFinishGroup, CoreMessages.dialog_connection_events_checkbox_wait_finish_timeout);
        return spinner;
    }

    private DBPConnectionEventType getSelectedEventType()
    {
        TableItem[] selection = eventTypeTable.getSelection();
        return ArrayUtils.isEmpty(selection) ? null : (DBPConnectionEventType) selection[0].getData();
    }

    private TableItem getEventItem(DBPConnectionEventType eventType)
    {
        for (TableItem item : eventTypeTable.getItems()) {
            if (item.getData() == eventType) {
                return item;
            }
        }
        return null;
    }

    private DBRShellCommand getActiveCommand() {
        DBPConnectionEventType eventType = getSelectedEventType();
        if (eventType != null) {
            DBRShellCommand command = eventsCache.get(eventType);
            if (command == null) {
                command = new DBRShellCommand(""); //$NON-NLS-1$
                eventsCache.put(eventType, command);
            }
            return command;
        }
        return null;
    }

    private void updateEvent(boolean commandChange)
    {
        DBPConnectionEventType eventType = getSelectedEventType();
        DBRShellCommand command = getActiveCommand();
        if (command != null) {
            boolean prevEnabled = command.isEnabled();
            if (commandChange) {
                command.setCommand(commandText.getText());
            } else {
                TableItem item = getEventItem(eventType);
                if (item != null) {
                    command.setEnabled(item.getChecked());
                }
                command.setShowProcessPanel(showProcessCheck.getSelection());
                command.setWaitProcessFinish(waitFinishCheck.getSelection());
                waitFinishTimeoutMs.setEnabled(waitFinishCheck.getSelection());
                command.setWaitProcessTimeoutMs(waitFinishTimeoutMs.getSelection());
                command.setTerminateAtDisconnect(terminateCheck.getSelection());
                command.setPauseAfterExecute(pauseAfterExecute.getSelection());
                command.setWorkingDirectory(workingDirectory.getText());
                if (prevEnabled != command.isEnabled()) {
                    selectEventType(eventType);
                }
            }
        } else if (!commandChange) {
            selectEventType(null);
        }
    }

    private void selectEventType(DBPConnectionEventType eventType)
    {
        DBRShellCommand command = eventType == null ? null : eventsCache.get(eventType);
        commandText.setEnabled(command != null && command.isEnabled());
        showProcessCheck.setEnabled(command != null && command.isEnabled());
        waitFinishCheck.setEnabled(command != null && command.isEnabled());
        waitFinishTimeoutMs.setEnabled(waitFinishCheck.isEnabled());
        terminateCheck.setEnabled(command != null && command.isEnabled());
        pauseAfterExecute.setEnabled(command != null && command.isEnabled());
        workingDirectory.setEnabled(command != null && command.isEnabled());
        workingDirectory.getTextControl().setEnabled(command != null && command.isEnabled());

        if (command != null) {
            commandText.setText(CommonUtils.toString(command.getCommand()));
            showProcessCheck.setSelection(command.isShowProcessPanel());
            waitFinishCheck.setSelection(command.isWaitProcessFinish());
            waitFinishTimeoutMs.setSelection(command.getWaitProcessTimeoutMs());
            terminateCheck.setSelection(command.isTerminateAtDisconnect());
            pauseAfterExecute.setSelection(command.getPauseAfterExecute());
            workingDirectory.setText(CommonUtils.notEmpty(command.getWorkingDirectory()));
        } else {
            commandText.setText(""); //$NON-NLS-1$
            showProcessCheck.setSelection(false);
            waitFinishCheck.setSelection(false);
            waitFinishTimeoutMs.setSelection(DBRShellCommand.WAIT_PROCESS_TIMEOUT_FOREVER);
            terminateCheck.setSelection(false);
            pauseAfterExecute.setSelection(0);
            workingDirectory.setText("");
        }
    }

    @Override
    public void saveSettings(DBPDataSourceContainer dataSourceDescriptor) {
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : eventsCache.entrySet()) {
            dataSourceDescriptor.getConnectionConfiguration().setEvent(entry.getKey(), entry.getValue());
        }
    }

}
