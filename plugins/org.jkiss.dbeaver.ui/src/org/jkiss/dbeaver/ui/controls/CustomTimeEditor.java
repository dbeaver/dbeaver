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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.data.formatters.TimestampFormatSample;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
	private final static String FORMAT_PATTERN = "pattern";
	private DateTime dateEditor;
	private DateTime timeEditor;
	private Composite basePart;
	private static final String TIMESTAMP_DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private String format = "";

	private static final Log log = Log.getLog(ViewerColumnController.class);

	public CustomTimeEditor(Composite parent, int style) {
		basePart = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		basePart.setLayout(layout);

		this.dateEditor = new DateTime(basePart, SWT.DATE | SWT.DROP_DOWN | style);
		GridData layoutDataDate = new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1);
		GridData layoutDataTime = new GridData(GridData.END, GridData.FILL, true, true, 1, 1);
		this.dateEditor.setLayoutData(layoutDataDate);
		this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.DROP_DOWN | style);
		this.timeEditor.setLayoutData(layoutDataTime);
		this.format = getTimestampFormat();
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
		Map<Object, Object> map = prefFormatt.getDefaultProperties(Locale.getDefault());
		Object pattern = map.get(FORMAT_PATTERN);
		if (pattern instanceof String) {
			format = (String) pattern;
			return format;
		}
		return TIMESTAMP_DEFAULT_FORMAT;
	}

	public void setValue(@Nullable String value) {
		Calendar calendar = Calendar.getInstance();
		if (value != null) {
			try {
				Date date = new SimpleDateFormat(format).parse((String) value);
				calendar.setTime(date);
			} catch (ParseException e) {
				log.error("Input value is null", e);
			}

		}
		dateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		timeEditor.setTime(calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	public String getValue() throws DBException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay(), timeEditor.getHours(),
				timeEditor.getMinutes(), timeEditor.getSeconds());

		return new SimpleDateFormat(format).format(calendar.getTime());

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
