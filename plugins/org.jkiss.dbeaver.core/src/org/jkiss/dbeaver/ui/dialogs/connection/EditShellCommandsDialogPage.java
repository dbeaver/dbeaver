/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.dialogs.connection;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.connection.DBPConnectionEventType;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.jobs.EventProcessorJob;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Events edit dialog page
 */
public class EditShellCommandsDialogPage extends ActiveWizardPage<ConnectionWizard> {

    private Text commandText;
    private Button showProcessCheck;
    private Button terminateCheck;
    private Button waitFinishCheck;
    private Table eventTypeTable;

    private final Map<DBPConnectionEventType, DBRShellCommand> eventsCache = new HashMap<>();

    protected EditShellCommandsDialogPage(DataSourceDescriptor dataSource)
    {
        super(CoreMessages.dialog_connection_events_title);
        setTitle("Shell Commands");
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
        Composite group = UIUtils.createPlaceholder(parent, 2);
        group.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite eventGroup = new Composite(group, SWT.NONE);
            eventGroup.setLayout(new GridLayout(1, false));
            eventGroup.setLayoutData(new GridData(GridData.FILL_VERTICAL));

            UIUtils.createControlLabel(eventGroup, CoreMessages.dialog_connection_events_label_event);
            eventTypeTable = new Table(eventGroup, SWT.BORDER | SWT.CHECK | SWT.SINGLE | SWT.FULL_SELECTION);
            eventTypeTable.setLayoutData(new GridData(GridData.FILL_VERTICAL));
            eventTypeTable.addListener(SWT.Selection, new Listener() {
                public void handleEvent(Event event) {
                    if (event.detail == SWT.CHECK) {
                        eventTypeTable.select(eventTypeTable.indexOf((TableItem) event.item));
                    }
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
            Composite detailsGroup = new Composite(group, SWT.NONE);
            detailsGroup.setLayout(new GridLayout(1, false));
            detailsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                //UIUtils.createControlGroup(group, "Event", 1, GridData.FILL_BOTH | GridData.HORIZONTAL_ALIGN_BEGINNING, 0);

            UIUtils.createControlLabel(detailsGroup, CoreMessages.dialog_connection_events_label_command);
            commandText = new Text(detailsGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
            commandText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e)
                {
                    updateEvent(true);
                }
            });
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
            terminateCheck = UIUtils.createCheckbox(detailsGroup, CoreMessages.dialog_connection_events_checkbox_terminate_at_disconnect, false);
            terminateCheck.addSelectionListener(eventEditAdapter);

            Group helpGroup = new Group(detailsGroup, SWT.NONE);
            helpGroup.setText("Command parameters");
            helpGroup.setLayout(new GridLayout(2, false));
            helpGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            Label infoLabel = new Label(helpGroup, SWT.NONE);
            infoLabel.setText("You may use following variables:");
            gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            infoLabel.setLayoutData(gd);
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_HOST, "target host");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_PORT, "target port");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_SERVER, "target server name");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_DATABASE, "target database");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_USER, "user name");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_PASSWORD, "password (plain)");
            addVariableLegend(helpGroup, EventProcessorJob.VARIABLE_URL, "JDBC URL");
        }

        selectEventType(null);

        setControl(group);
    }

    private void addVariableLegend(Composite group, String varName, String description) {
        Text nameText = new Text(group, SWT.READ_ONLY);
        nameText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
        nameText.setText("${" + varName + "}");

        Label descText = new Label(group, SWT.READ_ONLY);
        descText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        descText.setText("-" + description);
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

    private void updateEvent(boolean commandChange)
    {
        DBPConnectionEventType eventType = getSelectedEventType();
        if (eventType != null) {
            DBRShellCommand command = eventsCache.get(eventType);
            if (command == null) {
                command = new DBRShellCommand(""); //$NON-NLS-1$
                eventsCache.put(eventType, command);
            }
            boolean prevEnabled = command.isEnabled();
            if (commandChange) {
                command.setCommand(commandText.getText());
            } else {
                TableItem item = getEventItem(eventType);
                command.setEnabled(item.getChecked());
                command.setShowProcessPanel(showProcessCheck.getSelection());
                command.setWaitProcessFinish(waitFinishCheck.getSelection());
                command.setTerminateAtDisconnect(terminateCheck.getSelection());
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
        terminateCheck.setEnabled(command != null && command.isEnabled());

        if (command != null) {
            commandText.setText(CommonUtils.toString(command.getCommand()));
            showProcessCheck.setSelection(command.isShowProcessPanel());
            waitFinishCheck.setSelection(command.isWaitProcessFinish());
            terminateCheck.setSelection(command.isTerminateAtDisconnect());
        } else {
            commandText.setText(""); //$NON-NLS-1$
            showProcessCheck.setSelection(false);
            waitFinishCheck.setSelection(false);
            terminateCheck.setSelection(false);
        }
    }

    void saveConfigurations(DataSourceDescriptor dataSource)
    {
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : eventsCache.entrySet()) {
            dataSource.getConnectionConfiguration().setEvent(entry.getKey(), entry.getValue());
        }
    }

}
