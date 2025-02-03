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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObjectJDBC;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.sql.JDBCType;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
    private final int style;
    private final boolean isPanel;
    private DateTime dateEditor;
    private DateTime timeEditor;
    private Composite basePart;

    private Label timeLabel;
    private Label dateLabel;
    private int millis = -1;
    private String dateAsText = "";
    private final boolean isInline;

    private InputMode inputMode = InputMode.NONE;
    private final Calendar calendar = Calendar.getInstance();
    private Text textEditor;
    private Listener modifyListener;
    private SelectionAdapter selectionListener;
    private boolean editable;
    private CLabel warningLabel;
    private Composite mainComposite;
    private JDBCType jdbcType;

    private enum InputMode {
        NONE,
        DATE,
        TIME,
        DATETIME
    }

    public void createDateFormat(@NotNull DBSTypedObject valueType) {
        if (valueType instanceof DBSTypedObjectJDBC) {
            try {
                jdbcType = JDBCType.valueOf(valueType.getTypeID());
            } catch (Exception e) {
                jdbcType = JDBCType.TIMESTAMP;
            }
        } else {
            jdbcType = JDBCType.TIMESTAMP;
        }
        disposeNotNeededEditors();
    }

    public void createDateFormat(JDBCType jdbcType) {
        this.jdbcType = jdbcType;
        disposeNotNeededEditors();
    }

    private void disposeNotNeededEditors() {
        if (jdbcType != null) {
            switch (jdbcType) {
                case DATE:
                    inputMode = InputMode.DATE;
                    disposeEditor(timeEditor, timeLabel);
                    break;
                case TIME:
                    inputMode = InputMode.TIME;
                    disposeEditor(dateEditor, dateLabel);
                    break;
                default:
                    inputMode = InputMode.DATETIME;
                    break;
            }
        }
    }

    public CustomTimeEditor(@NotNull Composite parent, int style, boolean isPanel, boolean isInline) {
        this.isInline = isInline;
        this.isPanel = isPanel;
        this.style = style;
        initEditor(parent, style);
    }

    @NotNull
    private Composite initEditor(@NotNull Composite parent, int style) {

        if (!isInline) {
            GridLayout layout = new GridLayout(1, false);
            mainComposite = new Composite(parent, style);
            mainComposite.setLayout(layout);
            mainComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        }
        basePart = new Composite(isInline ? parent : mainComposite, style);
        basePart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        GridLayout basePartLayout = new GridLayout(2, false);
        if (isInline) {
            basePartLayout.marginHeight = 0;
            basePartLayout.marginWidth = 0;
        }
        basePart.setLayout(basePartLayout);
        setToDateComposite();


        //fixes calendar issues on inline mode
        basePart.pack();
        return isInline ? basePart : mainComposite;
    }

    /**
     * Disposes all DateTime editors and their labels and creates text editor
     */
    public void setToTextComposite() {
        if (textEditor != null && !textEditor.isDisposed()) {
            return;
        }
        disposeEditor(timeEditor, timeLabel);
        timeEditor = null;
        disposeEditor(dateEditor, dateLabel);
        dateEditor = null;
        textEditor = new Text(basePart, isPanel && !isInline ? style : SWT.BORDER);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        textEditor.setLayoutData(gridData);
        if (warningLabel != null) {
            warningLabel.dispose();
            warningLabel = null;
        }
        basePart.setLayoutData(new GridData(GridData.FILL_BOTH));
        allowEdit();
        textEditor.setText(dateAsText);
        if (mainComposite != null) {
            mainComposite.layout();
        }
        basePart.layout();
    }

    private void disposeEditor(Control editor, Label editorLabel) {
        if (editor != null) {
            editor.dispose();
            if (editorLabel != null) {
                editorLabel.dispose();
            }
        }
    }

    /**
     * Disposes text editor components and creates DateTime editors
     */
    public void setToDateComposite() {
        if (dateEditor != null || timeEditor != null) {
            return;
        }
        disposeEditor(textEditor, null);
        final GridData layoutData = new GridData(SWT.FILL, SWT.RIGHT, true, false, 1, 1);
        if (!isInline) {
            dateLabel = UIUtils.createLabel(basePart, "Date");
        }
        if (CommonUtils.isEmpty(dateAsText)) {
            updateWarningLabel(null);
        }
        this.dateEditor = new DateTime(basePart, SWT.DROP_DOWN);
        dateEditor.setLayoutData(layoutData);
        if (!isInline) {
            timeLabel = UIUtils.createLabel(basePart, "Time");
        }
        this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.MEDIUM);
        this.timeEditor.setLayoutData(layoutData);
        basePart.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        disposeNotNeededEditors();
        allowEdit();
        setDateFromCalendar();
        updateListeners();
        basePart.layout();
        if (mainComposite != null) {
            mainComposite.layout();
        }
    }

    public void updateListeners() {
        if (selectionListener != null) {
            if (dateEditor != null && !dateEditor.isDisposed()) {
                dateEditor.addSelectionListener(selectionListener);
            }
            if (timeEditor != null && !timeEditor.isDisposed()) {
                timeEditor.addSelectionListener(selectionListener);
            }
        }
        if (modifyListener != null && textEditor != null && !textEditor.isDisposed()) {
            textEditor.addListener(SWT.Modify, modifyListener);
        }
    }

    /**
     * Creates listeners for date editors.
     *
     * @param listener listener to add to all existing editors
     */
    public void addSelectionAdapter(@NotNull SelectionAdapter listener) {
        selectionListener = listener;
        updateListeners();
    }

    public void addModifyListener(@NotNull Listener listener) {
        modifyListener = listener;
        updateListeners();
    }

    private static void setWithoutListener(@NotNull Control control, Listener listener, @NotNull Runnable blockToRun) {
        if (listener != null) {
            control.removeListener(SWT.Modify, listener);
            blockToRun.run();
            control.addListener(SWT.Modify, listener);
        } else {
            blockToRun.run();
        }
    }


    private void updateWarningLabel(@Nullable Object value) {
        if (isInline) {
            return;
        }
        if (value == null && (textEditor == null || textEditor.isDisposed())) {
            if (warningLabel != null && !warningLabel.isDisposed()) {
                return;
            }
            warningLabel = new CLabel(mainComposite, style);
            warningLabel.setText("Original value was null, using current time");
            warningLabel.setImage(DBeaverIcons.getImage(DBIcon.SMALL_INFO));
            mainComposite.layout();
        } else {
            if (warningLabel != null) {
                warningLabel.dispose();
                mainComposite.layout();
            }
        }
    }

    public void setTextValue(@Nullable String value) {
        dateAsText = value;
        if (textEditor != null && !textEditor.isDisposed()) {
            setWithoutListener(textEditor, modifyListener, () -> textEditor.setText(dateAsText));
        }
    }

    /**
     * Sets value to the calendar
     *
     * @param value value to which calendar should be set
     * @throws DBCException if it can't be adapted to Date type
     */
    public void setValue(@Nullable Object value) throws DBCException {
        updateWarningLabel(value);
        Date adaptedValue = adaptToDate(value);
        if (adaptedValue != null) {
            calendar.setTime(adaptedValue);
        } else {
            switch (inputMode) {
                case TIME:
                    calendar.setTime(new Time(System.currentTimeMillis()));
                    break;
                case DATE:
                    calendar.setTime(new Date(System.currentTimeMillis()));
                    break;
                case DATETIME:
                    calendar.setTime(new Timestamp(System.currentTimeMillis()));
                    break;
                default:
                    break;
            }
        }
        setDateFromCalendar();
    }

    private void setDateFromCalendar() {
        if (dateEditor != null && !dateEditor.isDisposed()) {
            dateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
        }
        if (timeEditor != null && !timeEditor.isDisposed()) {
            timeEditor.addTraverseListener(e -> timeEditor.setFocus());
            timeEditor.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
            try {
                millis = calendar.get(Calendar.MILLISECOND);
            } catch (ArrayIndexOutOfBoundsException e) {
                //Calendar doesn't have any way to
                millis = -1;
            }
        }
    }

    @Nullable
    public String getValueAsString() {
        if (textEditor != null && !textEditor.isDisposed()) {
            return textEditor.getText();
        }
        return null;
    }

    @NotNull
    public Date getValueAsDate() {

        switch (inputMode) {
            case TIME:
                calendar.set(0, Calendar.JANUARY, 0, timeEditor.getHours(), timeEditor.getMinutes(), timeEditor.getSeconds());
                break;
            case DATE:
                calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay());
                break;
            case DATETIME:
                calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay(), timeEditor.getHours(),
                        timeEditor.getMinutes(), timeEditor.getSeconds());
                break;
            default:
                calendar.set(0, Calendar.JANUARY, 0, 0, 0, 0);
                break;
        }
        if (millis != -1) {
            calendar.set(Calendar.MILLISECOND, millis);
        }
        return calendar.getTime();
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
        allowEdit();
    }

    public void allowEdit() {
        if (this.dateEditor != null && !this.dateEditor.isDisposed()) {
            this.dateEditor.setEnabled(editable);
        }
        if (this.timeEditor != null && !this.timeEditor.isDisposed()) {
            this.timeEditor.setEnabled(editable);
        }
        if (this.textEditor != null && !this.textEditor.isDisposed()){
            this.textEditor.setEditable(editable);
        }
    }

    public Composite getControl() {
        return isInline ? basePart : mainComposite;
    }

    public void selectAllContent() {
        if (textEditor != null && !textEditor.isDisposed()) {
            textEditor.selectAll();
        }
    }

    /**
     * Adapts values to date
     *
     * @param value value which should be adapted
     * @return adapted value
     * @throws DBCException when it can't be adapted, should be shown as text in presentation
     */
    @Nullable
    public Date adaptToDate(@Nullable Object value) throws DBCException {
        if (value == null) {
            return null;
        } else if (value instanceof Date) {
            return (Date) value;
        } else if (value instanceof Instant) {
            return Date.from((Instant) value);
        } else if (value instanceof LocalDateTime) {
            return Date.from(((LocalDateTime) value).atZone(ZoneId.systemDefault()).toInstant());
        } else {
            throw new DBCException(value.toString());
        }
    }
}
