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

import java.util.Calendar;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDDataFormatter;

/**
 * CustomTimeEditor
 */
public class DateTimeEditor {

	private Composite basePart;

	private DateTime dateEditor;
	private DateTime timeEditor;
	private String formaterId;

	private static final Log log = Log.getLog(ViewerColumnController.class);

	public DateTimeEditor(Composite parent, int style, String formaterId) {

		if (formaterId == null || formaterId.isEmpty()) {
			formaterId = DBDDataFormatter.TYPE_NAME_TIMESTAMP;
		}
		this.formaterId = formaterId;
		basePart = new Composite(parent, SWT.BORDER);

		GridLayout layout = new GridLayout(1, true);
		layout.marginHeight = 1;
		layout.marginWidth = 1;
		layout.horizontalSpacing = 1;
		layout.verticalSpacing = 1;
		basePart.setLayout(layout);

		GridData dateData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);
		GridData timeData = new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1);

		if (formaterId.equals(DBDDataFormatter.TYPE_NAME_TIMESTAMP)) {
			this.dateEditor = new DateTime(basePart, SWT.DATE | SWT.DROP_DOWN | style);
			this.dateEditor.setLayoutData(dateData);

			this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.DROP_DOWN | style);
			this.timeEditor.setLayoutData(timeData);
		} else if (formaterId.equals(DBDDataFormatter.TYPE_NAME_DATE)) {
			this.dateEditor = new DateTime(basePart, SWT.DATE | SWT.DROP_DOWN | style);
			this.dateEditor.setLayoutData(dateData);
		} else if (formaterId.equals(DBDDataFormatter.TYPE_NAME_TIME)) {
			this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.DROP_DOWN | style);
			this.timeEditor.setLayoutData(timeData);
		}

	}

	public void addSelectionAdapter(SelectionAdapter listener) {
		if (dateEditor != null && !dateEditor.isDisposed()) {
			dateEditor.addSelectionListener(listener);
		}
		if (timeEditor != null && !timeEditor.isDisposed()) {
			timeEditor.addSelectionListener(listener);
		}
	}

	public void addTraversedListener(TraverseListener listener) {
		if (dateEditor != null && !dateEditor.isDisposed()) {
			dateEditor.addTraverseListener(listener);
		}
		if (timeEditor != null && !timeEditor.isDisposed()) {
			timeEditor.addTraverseListener(listener);
		}
	}

	public void setValue(@Nullable Date value) {
		Calendar calendar = Calendar.getInstance();
		if (value != null) {
			calendar.setTime(value);

		}

		if (dateEditor != null && !dateEditor.isDisposed()) {
			dateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
					calendar.get(Calendar.DAY_OF_MONTH));

		}
		if (timeEditor != null && !timeEditor.isDisposed()) {
			timeEditor.setTime(calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE),
					calendar.get(Calendar.SECOND));

		}
	}

	public Date getValue() throws DBException {
		Calendar calendar = Calendar.getInstance();

		if (formaterId.equals(DBDDataFormatter.TYPE_NAME_TIMESTAMP)) {
			calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay(), timeEditor.getHours(),
					timeEditor.getMinutes(), timeEditor.getSeconds());
		} else if (formaterId.equals(DBDDataFormatter.TYPE_NAME_DATE)) {
			calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay());

		} else if (formaterId.equals(DBDDataFormatter.TYPE_NAME_TIME)) {
			calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
					timeEditor.getHours(), timeEditor.getMinutes(), timeEditor.getSeconds());
		}

		return calendar.getTime();
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
		return null; // return null for restrict adding listeners in the multiContolEditor
	}

	public void selectAll() {
	}

}
