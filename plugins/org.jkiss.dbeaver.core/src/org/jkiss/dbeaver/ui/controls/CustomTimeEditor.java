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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.util.Calendar;
import java.util.Date;

/**
 * CustomTimeEditor
 */
public class CustomTimeEditor {
	private DateTime dateEditor;
	private DateTime timeEditor;
	private Composite basePart;

	private static final Log log = Log.getLog(ViewerColumnController.class);

	public CustomTimeEditor(Composite parent, int style) {
		basePart = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		basePart.setLayout(layout);

		GridData dateGD = new GridData(SWT.FILL,SWT.FILL,false,false,1,1);
		dateGD.widthHint=110;
		GridData timeGD = new GridData(SWT.FILL,SWT.FILL,false,false,1,1);
		timeGD.widthHint=90;
		
		this.dateEditor = new DateTime(basePart, SWT.DATE | SWT.LONG | SWT.DROP_DOWN | style);
		this.dateEditor.setLayoutData(dateGD);
		
		this.timeEditor = new DateTime(basePart, SWT.TIME | SWT.DROP_DOWN | style);
		this.timeEditor.setLayoutData(timeGD);
	}

	public void addSelectionAdapter(SelectionAdapter listener) {
		if (dateEditor != null && !dateEditor.isDisposed()) {
			dateEditor.addSelectionListener(listener);
		}
		if (timeEditor != null && !timeEditor.isDisposed()) {
			timeEditor.addSelectionListener(listener);
		}
	}

	public void setValue(@Nullable Date value) {
		Calendar calendar = Calendar.getInstance();
		if (value != null) {
            calendar.setTime(value);

		}
		dateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH),
				calendar.get(Calendar.DAY_OF_MONTH));
		timeEditor.setTime(calendar.get(Calendar.HOUR), calendar.get(Calendar.MINUTE), calendar.get(Calendar.SECOND));
	}

	public Date getValue() throws DBException {
		Calendar calendar = Calendar.getInstance();
		calendar.set(dateEditor.getYear(), dateEditor.getMonth(), dateEditor.getDay(), timeEditor.getHours(),
				timeEditor.getMinutes(), timeEditor.getSeconds());

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
		return basePart;
	}

	public void selectAll() {
	}
}
