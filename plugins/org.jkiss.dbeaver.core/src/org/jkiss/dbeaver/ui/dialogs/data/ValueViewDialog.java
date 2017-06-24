/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.dialogs.data;

import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.*;
import org.jkiss.dbeaver.ui.data.managers.BaseValueManager;
import org.jkiss.dbeaver.ui.dialogs.ColumnInfoPanel;
import org.jkiss.utils.CommonUtils;

/**
 * ValueViewDialog
 *
 * @author Serge Rider
 */
public abstract class ValueViewDialog extends Dialog implements IValueEditorStandalone {

    private static final Log log = Log.getLog(ValueViewDialog.class);

    private static int dialogCount = 0;
    public static final String SETTINGS_SECTION_DI = "ValueViewDialog";

    private IValueController valueController;
    private Object editedValue;
    private boolean columnInfoVisible = true;
    private ColumnInfoPanel columnPanel;
    private final IDialogSettings dialogSettings;
    private boolean opened;

    protected ValueViewDialog(IValueController valueController) {
        super(valueController.getValueSite().getShell());
        setShellStyle(SWT.SHELL_TRIM);
        this.valueController = valueController;
        dialogSettings = UIUtils.getDialogSettings(SETTINGS_SECTION_DI);
        if (dialogSettings.get(getInfoVisiblePrefId()) != null) {
            columnInfoVisible = dialogSettings.getBoolean(getInfoVisiblePrefId());
        }
        dialogCount++;
    }

    @Override
    public void createControl() {

    }

    protected IDialogSettings getDialogSettings()
    {
        return dialogSettings;
    }

    @Nullable
    protected IValueEditor createPanelEditor(final Composite placeholder)
        throws DBException
    {
        IValueEditor editor = valueController.getValueManager().createEditor(new IValueController() {
            @NotNull
            @Override
            public DBCExecutionContext getExecutionContext() {
                return valueController.getExecutionContext();
            }

            @Override
            public String getValueName()
            {
                return valueController.getValueName();
            }

            @Override
            public DBSTypedObject getValueType()
            {
                return valueController.getValueType();
            }

            @Override
            public Object getValue()
            {
                return valueController.getValue();
            }

            @Override
            public void updateValue(Object value, boolean updatePresentation)
            {
                valueController.updateValue(value, updatePresentation);
            }

            @Override
            public DBDValueHandler getValueHandler()
            {
                return valueController.getValueHandler();
            }

            @Override
            public IValueManager getValueManager() {
                return valueController.getValueManager();
            }

            @Override
            public EditType getEditType()
            {
                return EditType.PANEL;
            }

            @Override
            public boolean isReadOnly()
            {
                return valueController.isReadOnly();
            }

            @Override
            public IWorkbenchPartSite getValueSite()
            {
                return valueController.getValueSite();
            }

            @Override
            public Composite getEditPlaceholder()
            {
                return placeholder;
            }

            @Override
            public void refreshEditor() {
                valueController.refreshEditor();
            }

            @Override
            public void showMessage(String message, DBPMessageType messageType)
            {
            }
        });
        if (editor != null) {
            editor.createControl();
        }
        return editor;
    }

    public IValueController getValueController() {
        return valueController;
    }

    @Override
    public void showValueEditor() {
        if (!opened) {
            open();
        } else {
            getShell().setFocus();
        }
    }

    @Override
    public void closeValueEditor() {
        this.valueController = null;
        this.setReturnCode(CANCEL);
        this.close();
    }

    @Override
    protected Control createDialogArea(Composite parent)
    {
/*
        SashForm sash = new SashForm(parent, SWT.VERTICAL);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));
        Composite dialogGroup = (Composite)super.createDialogArea(sash);
        dialogGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        new ColumnInfoPanel(dialogGroup, SWT.BORDER, getValueController());
        Composite editorGroup = (Composite) super.createDialogArea(sash);
        editorGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
        //editorGroup.setLayout(new GridLayout(1, false));
        return editorGroup;

*/
        Composite dialogGroup = (Composite)super.createDialogArea(parent);
        if (valueController instanceof IAttributeController) {
            final Link columnHideLink = new Link(dialogGroup, SWT.NONE);
            columnHideLink.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e)
                {
                    columnInfoVisible = !columnInfoVisible;
                    dialogSettings.put(getInfoVisiblePrefId(), columnInfoVisible);
                    initColumnInfoVisibility(columnHideLink);
                    getShell().layout();
                    int width = getShell().getSize().x;
                    getShell().setSize(width, getShell().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
                }
            });

            columnPanel = new ColumnInfoPanel(dialogGroup, SWT.BORDER, valueController);
            columnPanel.setLayoutData(new GridData(GridData.FILL_BOTH));

            initColumnInfoVisibility(columnHideLink);
        }

        return dialogGroup;
    }

    private void initColumnInfoVisibility(Link columnHideLink)
    {
        columnPanel.setVisible(columnInfoVisible);
        ((GridData)columnPanel.getLayoutData()).exclude = !columnInfoVisible;
        columnHideLink.setText("Column Info: (<a>" + (columnInfoVisible ? "hide" : "show") + "</a>)");
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        // create OK and Cancel buttons by default
        createButton(parent, IDialogConstants.OK_ID, CoreMessages.dialog_value_view_button_save, true)
            .setEnabled(!valueController.isReadOnly());
        boolean required = false;//valueController.getValueType() instanceof DBSAttributeBase && ((DBSAttributeBase) valueController.getValueType()).isRequired();
        createButton(parent, IDialogConstants.IGNORE_ID, CoreMessages.dialog_value_view_button_sat_null, false)
            .setEnabled(!valueController.isReadOnly() && !DBUtils.isNullValue(valueController.getValue()) && !required);
        createButton(parent, IDialogConstants.CANCEL_ID, CoreMessages.dialog_value_view_button_cancel, false);
    }

    @Override
    protected void initializeBounds()
    {
        super.initializeBounds();

        Shell shell = getShell();

        String sizeString = dialogSettings.get(getDialogSizePrefId());
        if (!CommonUtils.isEmpty(sizeString) && sizeString.contains(":")) {
            int divPos = sizeString.indexOf(':');
            shell.setSize(new Point(
                Integer.parseInt(sizeString.substring(0, divPos)),
                Integer.parseInt(sizeString.substring(divPos + 1))));
            shell.layout();
        }

        Monitor primary = shell.getMonitor();
        Rectangle bounds = primary.getBounds ();
        Rectangle rect = shell.getBounds ();
        int x = bounds.x + (bounds.width - rect.width) / 2;
        int y = bounds.y + (bounds.height - rect.height) / 3;
        x += dialogCount * 20;
        y += dialogCount * 20;
        shell.setLocation(x, y);
    }

    private String getInfoVisiblePrefId()
    {
        return getClass().getSimpleName() + "-" +
            CommonUtils.escapeIdentifier(getValueController().getValueType().getTypeName()) +
            "-columnInfoVisible";
    }

    private String getDialogSizePrefId()
    {
        return getClass().getSimpleName() + "-" +
            CommonUtils.escapeIdentifier(getValueController().getValueType().getTypeName()) +
            "-dialogSize";
    }

    @Override
    public final int open()
    {
        try {
            opened = true;
            int result = super.open();
            if (result == IDialogConstants.OK_ID) {
                getValueController().updateValue(editedValue, true);
            }
            return result;
        } finally {
            dialogCount--;
            this.valueController = null;
        }
    }

    @Override
    protected void okPressed()
    {
        try {
            editedValue = extractEditorValue();

            super.okPressed();
        }
        catch (Exception e) {
            DBUserInterface.getInstance().showError(CoreMessages.dialog_value_view_dialog_error_updating_title, CoreMessages.dialog_value_view_dialog_error_updating_message, e);
            super.cancelPressed();
        }
    }

    @Override
    protected void buttonPressed(int buttonId) {
        Point size = getShell().getSize();
        String sizeString = size.x + ":" + size.y;
        dialogSettings.put(getDialogSizePrefId(), sizeString);

        if (buttonId == IDialogConstants.IGNORE_ID) {
            if (!valueController.isReadOnly()) {
                editedValue = BaseValueManager.makeNullValue(valueController);
            }
            super.okPressed();
        } else {
            super.buttonPressed(buttonId);
        }
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        if (valueController instanceof IAttributeController) {
            DBSAttributeBase meta = ((IAttributeController)valueController).getBinding();
            shell.setText(meta.getName());
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {

    }
}
