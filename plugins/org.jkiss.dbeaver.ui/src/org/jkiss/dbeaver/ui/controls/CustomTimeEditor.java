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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.impl.data.formatters.TimestampFormatSample;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.JDBCType;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
    private final static String FORMAT_PATTERN = "pattern";
    private final int style;
    private final boolean isPanel;
    private DateTime dateEditor;
    private DateTime timeEditor;
    private Composite basePart;

    private static final String TIMESTAMP_DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private String format = "";
    private Label timeLabel;
    private Label dateLabel;
    private int millis = -1;
    private String dateAsText = "";
    private final boolean isInline;

    private InputMode inputMode = InputMode.None;
    private final Calendar calendar = Calendar.getInstance();
    private Text textEditor;
    private Listener modifyListener;
    private SelectionAdapter selectionListener;
    private boolean editable;

    private enum InputMode {
        None,
        Date,
        Time,
        DateTime
    }

    public void createDateFormat(@NotNull DBSTypedObject valueType) {
        final JDBCType jdbcType = JDBCType.valueOf(valueType.getTypeID());
        switch (jdbcType) {
            case DATE:
                inputMode = InputMode.Date;
                disposeDateEditor(timeEditor, timeLabel);
                break;
            case TIME:
                inputMode = InputMode.Time;
                disposeDateEditor(dateEditor, dateLabel);
                break;
            default:
                inputMode = InputMode.DateTime;
                break;
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
        basePart = new Composite(parent, style);
        GridLayout layout = new GridLayout(2, false);
        if (isInline) {
            layout.marginWidth = 0;
            layout.marginHeight = 0;
        }
        basePart.setLayout(layout);
        setToDateComposite();


        //fixes calendar issues on inline mode
        basePart.pack();

        this.format = getTimestampFormat();
        return basePart;
    }

    /**
     * Disposes all DateTime editors and their labels and creates text editor
     */
    public void setToTextComposite() {
        if (textEditor != null && !textEditor.isDisposed()) {
            return;
        }
        disposeDateEditor(timeEditor, timeLabel);
        timeEditor = null;
        disposeDateEditor(dateEditor, dateLabel);
        dateEditor = null;
        textEditor = new Text(basePart, isPanel && !isInline ? style : style | SWT.BORDER);
        final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1);
        textEditor.setLayoutData(gridData);
        allowEdit();
        textEditor.setText(dateAsText);
        basePart.layout();
    }

    private void disposeDateEditor(DateTime dateTimeEditor, Label dateTimeLabel) {
        if (dateTimeEditor != null) {
            dateTimeEditor.dispose();
            if (dateTimeLabel != null) {
                dateTimeLabel.dispose();

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
        if (textEditor != null && !textEditor.isDisposed()) {
            textEditor.dispose();
            textEditor = null;
        }
        final GridData layoutData = new GridData(SWT.FILL, SWT.RIGHT, true, false, 1, 1);
        if (!isInline) dateLabel = UIUtils.createLabel(basePart, "Date");
        this.dateEditor = new DateTime(basePart, SWT.DROP_DOWN);
        dateEditor.setLayoutData(layoutData);
        if (!isInline) timeLabel = UIUtils.createLabel(basePart, "Time");
        this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.MEDIUM);
        this.timeEditor.setLayoutData(layoutData);
        allowEdit();

        setDateFromCalendar();
        updateListeners();
        basePart.layout();
    }

    public void setFormat(@NotNull String format) {
        this.format = format;
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

    private static void setWithoutListener(@NotNull Control control, int type, Listener listener, @NotNull Runnable blockToRun) {
        if (listener != null) {
            control.removeListener(type, listener);
            blockToRun.run();
            control.addListener(type, listener);
        } else {
            blockToRun.run();
        }
    }

    private String getTimestampFormat() {
        TimestampFormatSample prefFormat = new TimestampFormatSample();
        Map<String, Object> map = prefFormat.getDefaultProperties(Locale.getDefault());
        Object pattern = map.get(FORMAT_PATTERN);
        if (pattern instanceof String) {
            format = (String) pattern;
            return format;
        }
        return TIMESTAMP_DEFAULT_FORMAT;
    }

    public void setTextValue(@Nullable String value) {
        dateAsText = value;
        if (textEditor != null && !textEditor.isDisposed()) {
            setWithoutListener(textEditor, SWT.Modify, modifyListener, () -> {
                textEditor.setText(value);
            });
        }
    }

    public void setValue(@Nullable Date value) {
        if (value != null) {
            calendar.setTime(value);
        } else {
            calendar.setTime(new Date());
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

    @Nullable
    public Date getValueAsDate() {

        switch (inputMode) {
            case Time:
                calendar.set(0, Calendar.JANUARY, 0, timeEditor.getHours(), timeEditor.getMinutes(), timeEditor.getSeconds());
                break;
            case Date:
                calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay());
                break;
            case DateTime:
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
        return basePart;
    }

    public void selectAllContent() {
        if (textEditor != null && !textEditor.isDisposed()) {
            textEditor.selectAll();
        }
    }

}
