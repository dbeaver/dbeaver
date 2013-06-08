/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
import org.jkiss.dbeaver.model.DBPConnectionEventType;
import org.jkiss.dbeaver.model.DBPConnectionInfo;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Events edit dialog page
 */
public class EditEventsDialogPage extends ActiveWizardPage<ConnectionWizard> {

    private DBPConnectionInfo connectionInfo;
    private Text commandText;
    private Button showProcessCheck;
    private Button terminateCheck;
    private Button waitFinishCheck;
    private Table eventTypeTable;

    private final Map<DBPConnectionEventType, DBRShellCommand> eventsCache = new HashMap<DBPConnectionEventType, DBRShellCommand>();

    protected EditEventsDialogPage(DBPConnectionInfo connectionInfo)
    {
        super(CoreMessages.dialog_connection_events_title);
        setTitle("Events");
        setDescription(CoreMessages.dialog_connection_events_title);
        setImageDescriptor(DBIcon.EVENT.getImageDescriptor());
        this.connectionInfo = connectionInfo;
        for (DBPConnectionEventType eventType : DBPConnectionEventType.values()) {
            DBRShellCommand command = connectionInfo.getEvent(eventType);
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

            for (DBPConnectionEventType eventType : DBPConnectionEventType.values()) {
                DBRShellCommand command = eventsCache.get(eventType);
                TableItem item = new TableItem(eventTypeTable, SWT.NONE);
                item.setData(eventType);
                item.setText(eventType.getTitle());
                item.setImage(DBIcon.EVENT.getImage());
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
        }

        selectEventType(null);

        setControl(group);
    }

    private DBPConnectionEventType getSelectedEventType()
    {
        TableItem[] selection = eventTypeTable.getSelection();
        return CommonUtils.isEmpty(selection) ? null : (DBPConnectionEventType) selection[0].getData();
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

    void saveConfigurations()
    {
        for (Map.Entry<DBPConnectionEventType, DBRShellCommand> entry : eventsCache.entrySet()) {
            connectionInfo.setEvent(entry.getKey(), entry.getValue());
        }
    }

}
