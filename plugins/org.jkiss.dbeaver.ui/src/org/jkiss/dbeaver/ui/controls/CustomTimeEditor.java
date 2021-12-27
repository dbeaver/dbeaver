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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.lsp4e.ui.UI;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.impl.data.formatters.TimestampFormatSample;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.UIUtils;

import java.sql.JDBCType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
    private final static String FORMAT_PATTERN = "pattern";
    private final DateTime dateEditor;
    private final DateTime timeEditor;
    private final Composite basePart;

    private static final String TIMESTAMP_DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private String format = "";
    private DateFormat dateFormat = null;
    private final Label timeLabel;
    private final Label dateLabel;
    private int millis = -1;


    InputMode inputMode = InputMode.None;
    private final Calendar calendar = Calendar.getInstance();

    private enum InputMode {
        None,
        Date,
        Time,
        DateTime
    }

    public void setDateFormat(DateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    public void createDateFormat(@NotNull DBSTypedObject valueType) {
        final JDBCType jdbcType = JDBCType.valueOf(valueType.getTypeID());
        switch (jdbcType) {
            case DATE:
                setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));

                inputMode = InputMode.Date;
                timeEditor.dispose();
                timeLabel.dispose();
                break;
            case TIME_WITH_TIMEZONE:
                setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSX"));
                inputMode = InputMode.DateTime;
                break;
            case TIME:
                setDateFormat(new SimpleDateFormat("HH:mm:ss"));
                inputMode = InputMode.Time;
                dateEditor.dispose();
                dateLabel.dispose();
                break;
            default:
                setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS"));
                inputMode = InputMode.DateTime;
                break;
        }
    }

    public CustomTimeEditor(Composite parent, int style, boolean isPanel) {

        basePart = new Composite(parent, style);
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 10;
        basePart.setLayout(layout);
        basePart.setFocus();
        final GridData layoutData = new GridData(SWT.FILL, isPanel ? SWT.UP : SWT.RIGHT, true, false, 1, 1);
        dateLabel = UIUtils.createLabel(basePart,"Date");
        this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.MEDIUM | SWT.NO_FOCUS);
        this.timeEditor.setLayoutData(layoutData);
        timeEditor.setFocus();
        timeLabel = UIUtils.createLabel(basePart, "Time");
        this.dateEditor = new DateTime(basePart, SWT.DROP_DOWN | SWT.NO_FOCUS);
        dateEditor.setLayoutData(layoutData);
        this.format = getTimestampFormat();
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void addSelectionAdapter(SelectionAdapter listener) {
        if (dateEditor != null && !dateEditor.isDisposed()) {
            dateEditor.addSelectionListener(listener);
        }
        if (timeEditor != null && !timeEditor.isDisposed()) {
            timeEditor.addSelectionListener(listener);
        }
    }

    private String getTimestampFormat() {
        TimestampFormatSample prefFormatt = new TimestampFormatSample();
        Map<String, Object> map = prefFormatt.getDefaultProperties(Locale.getDefault());
        Object pattern = map.get(FORMAT_PATTERN);
        if (pattern instanceof String) {
            format = (String) pattern;
            return format;
        }
        return TIMESTAMP_DEFAULT_FORMAT;
    }

    public void setValue(@Nullable Date value) {
        if (value != null) {
            calendar.setTime(value);
        }
        if (!dateEditor.isDisposed()) {
            dateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));

        }
        if (!timeEditor.isDisposed()) {
            timeEditor.addTraverseListener(e -> {
                timeEditor.setFocus();
            });
            timeEditor.setTime(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));

            try {
                millis = calendar.get(Calendar.MILLISECOND);
            } catch (ArrayIndexOutOfBoundsException e) {
                //Calendar doesn't have any way to
                millis = -1;
            }
        }

    }

    public String getValue() throws DBException {
        switch (inputMode) {
            case Time:
                calendar.set(0, 0, 0, timeEditor.getHours(), timeEditor.getMinutes(), timeEditor.getSeconds());
                break;
            case Date:
                calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay());
                break;
            case DateTime:
                calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay(), timeEditor.getHours(),
                    timeEditor.getMinutes(), timeEditor.getSeconds());
                break;
        }
        if (millis != -1) {
            calendar.set(Calendar.MILLISECOND, millis);
        }
        return dateFormat.format(calendar.getTime());
    }

    public void setEditable(boolean editable) {
        if (this.dateEditor != null && !this.dateEditor.isDisposed()) {
            this.dateEditor.setEnabled(editable);
        }

        if (this.timeEditor != null && !this.timeEditor.isDisposed()) {
            this.timeEditor.setEnabled(editable);
        }
    }

    public Composite getControl() {
        return basePart;
    }

    public void selectAll() {
    }
}
