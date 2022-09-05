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
package org.jkiss.dbeaver.ui.actions.common;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.*;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.navigator.actions.NavigatorHandlerObjectBase;

import java.util.UUID;

public class GenerateUUIDHandler extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart == null) {
            return null;
        }
        String uuid = generateUUID();

        IResultSetController rsc = activePart.getAdapter(IResultSetController.class);
        if (rsc != null && UIUtils.hasFocus(rsc.getControl())) {
            IResultSetSelection selection = rsc.getSelection();
            if (selection != null && !selection.isEmpty()) {
                for (Object cell : selection.toArray()) {
                    DBDAttributeBinding attr = selection.getElementAttribute(cell);
                    ResultSetRow row = selection.getElementRow(cell);
                    if (row != null && attr != null) {
                        ResultSetValueController valueController = new ResultSetValueController(
                            rsc,
                            new ResultSetCellLocation(attr, row),
                            IValueController.EditType.NONE,
                            null);
                        //DBDValueHandler valueHandler = valueController.getValueHandler();
                        valueController.updateValue(uuid, false);
                    }
                }
                rsc.redrawData(false, false);
                rsc.updateEditControls();
            }
        } else {
            ITextViewer textViewer = activePart.getAdapter(ITextViewer.class);
            if (textViewer != null) {
                ISelection selection = textViewer.getSelectionProvider().getSelection();
                if (selection instanceof TextSelection) {
                    try {
                        int offset = ((TextSelection) selection).getOffset();
                        int length = ((TextSelection) selection).getLength();
                        textViewer.getDocument().replace(
                            offset,
                            length, uuid);
                        textViewer.getSelectionProvider().setSelection(new TextSelection(offset + uuid.length(), 0));
                    } catch (BadLocationException e) {
                        DBWorkbench.getPlatformUI().showError("Insert UUID", "Error inserting UUID in text editor", e);
                    }
                }
            } else {
                Clipboard clipboard = new Clipboard(Display.getCurrent());
                try {
                    TextTransfer textTransfer = TextTransfer.getInstance();
                    clipboard.setContents(
                        new Object[]{uuid},
                        new Transfer[]{textTransfer});
                    DBeaverNotifications.showNotification(
                        "uuid-generator",
                        CoreMessages.notification_org_jkiss_dbeaver_ui_actions_common_uuid_copy,
                        CoreMessages.notification_org_jkiss_dbeaver_ui_actions_common_uuid_copy_text,
                        DBPMessageType.INFORMATION,
                        null
                    );
                } finally {
                    clipboard.dispose();
                }
            }
        }

        return null;
    }

    private static String generateUUID() {
        return UUID.randomUUID().toString();
    }

}