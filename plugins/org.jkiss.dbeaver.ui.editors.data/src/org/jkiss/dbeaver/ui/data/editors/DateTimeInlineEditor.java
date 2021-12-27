/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.compare.internal.ViewerDescriptor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.data.IValueController;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

/**
* DateTimeInlineEditor
*/
public class DateTimeInlineEditor extends BaseValueEditor<Control> {
    private CustomTimeEditor timeEditor;

    public DateTimeInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Control createControl(Composite editPlaceholder) {
        Object value = valueController.getValue();
        valueController.getEditPlaceholder();

        timeEditor = new CustomTimeEditor(
            editPlaceholder,
           SWT.MULTI, false);
        timeEditor.addSelectionAdapter(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dirty = true;
                Event selectionEvent = new Event();
                selectionEvent.widget = timeEditor.getControl();
                timeEditor.getControl().notifyListeners(SWT.Selection, selectionEvent);
            }
        });
        primeEditorValue(value);
        timeEditor.createDateFormat(valueController.getValueType());
        timeEditor.setEditable(!valueController.isReadOnly());



        return timeEditor.getControl();
    }

    @Override
    public Object extractEditorValue() throws DBException {
        try (DBCSession session = valueController.getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make datetime value from editor")) {
            final String strValue = timeEditor.getValue();
            return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(), strValue, false, false);
        }
    }

    @Override
    public void primeEditorValue(@Nullable Object value)
    {
        if (value instanceof Time) {
            timeEditor.setValue((Time) value);
        } else if (value instanceof Timestamp) {
            timeEditor.setValue((Timestamp) value);
        } else if (value instanceof Date) {
            timeEditor.setValue((Date) value);
        }
        if (valueController.getEditType() == IValueController.EditType.INLINE) {
            timeEditor.selectAll();
        }
    }

}
