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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.data.IValueController;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;

/**
 * DateTimeInlineEditor
 */
public class DateTimeInlineEditor extends BaseValueEditor<Control> {
    private TextMode textMode;
    private DateEditorMode dateEditorMode;
    private CustomTimeEditor timeEditor;


    /**
     * Action which sets edit mode to string edit
     */
    private static class TextMode extends Action {
        CustomTimeEditor editor;

        TextMode(CustomTimeEditor editor) {
            super("Text", Action.AS_RADIO_BUTTON);
            this.editor = editor;
            super.setText("Text");
            super.setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SQL_TEXT));
        }

        @Override
        public void run() {
            super.run();
            editor.setToTextComposite();
            ModelPreferences.getPreferences().setValue(ModelPreferences.RESULT_SET_USE_DATETIME_EDITOR, false);
        }
    }

    /**
     * Action which selects datetime mode
     */
    private static class DateEditorMode extends Action {
        CustomTimeEditor editor;


        DateEditorMode(CustomTimeEditor editor) {
            super("Calendar", Action.AS_RADIO_BUTTON);
            this.editor = editor;
            super.setText("Calendar");
            super.setImageDescriptor(DBeaverIcons.getImageDescriptor(DBIcon.TYPE_DATETIME));
        }

        @Override
        public void run() {
            super.run();
            editor.setToDateComposite();
            ModelPreferences.getPreferences().setValue(ModelPreferences.RESULT_SET_USE_DATETIME_EDITOR, true);
        }

    }

    public DateTimeInlineEditor(IValueController controller) {
        super(controller);
    }

    @Override
    protected Control createControl(Composite editPlaceholder) {
        Object value = valueController.getValue();
        valueController.getEditPlaceholder();
        boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;
        timeEditor = new CustomTimeEditor(
                editPlaceholder,
                SWT.MULTI, true, inline);
        textMode = new TextMode(timeEditor);
        dateEditorMode = new DateEditorMode(timeEditor);
        if (!isCalendarMode()) {
            textMode.run();
            textMode.setChecked(true);
        } else dateEditorMode.setChecked(true);

        timeEditor.addSelectionAdapter(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                dirty = true;
                Event selectionEvent = new Event();
                selectionEvent.widget = timeEditor.getControl();
                timeEditor.getControl().notifyListeners(SWT.Selection, selectionEvent);
            }
        });
        timeEditor.addModifyListener(e -> {
            dirty = true;
            Event modificationEvent = new Event();
            modificationEvent.widget = timeEditor.getControl();
            timeEditor.getControl().notifyListeners(SWT.Modify, modificationEvent);
        });
        primeEditorValue(value);
        timeEditor.createDateFormat(valueController.getValueType());
        timeEditor.setEditable(!valueController.isReadOnly());


        return timeEditor.getControl();
    }

    private boolean isCalendarMode() {
        return ModelPreferences.getPreferences().getBoolean(ModelPreferences.RESULT_SET_USE_DATETIME_EDITOR);
    }

    @Override
    public Object extractEditorValue() throws DBException {
        try (DBCSession session = valueController.getExecutionContext().openSession(new VoidProgressMonitor(), DBCExecutionPurpose.UTIL, "Make datetime value from editor")) {
            if (!isCalendarMode()) {
                final String strValue = timeEditor.getValueAsString();
                return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(), strValue, false, true);
            } else {
                final Date dateValue = timeEditor.getValueAsDate();
                return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(), dateValue, false, true);
            }
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        super.contributeActions(manager, controller);
        manager.add(ActionUtils.makeActionContribution(textMode, false));
        manager.add(ActionUtils.makeActionContribution(dateEditorMode, false));
        manager.update(true);
        timeEditor.getControl().layout();
    }

    @Override
    public void primeEditorValue(@Nullable Object value) {
        if (value == null) {
            return;
        }
        timeEditor.setTextValue(valueController.getValueHandler().getValueDisplayString(valueController.getValueType(), value, DBDDisplayFormat.EDIT));
        if (value instanceof Time) {
            timeEditor.setValue((Time) value);
        } else if (value instanceof Timestamp) {
            timeEditor.setValue((Timestamp) value);
        } else if (value instanceof Date) {
            timeEditor.setValue((Date) value);
        }
    }

}
