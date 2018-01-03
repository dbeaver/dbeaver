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
package org.jkiss.dbeaver.ui.data.editors;

import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.data.DateTimeCustomValueHandler;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.controls.CustomTimeEditor;
import org.jkiss.dbeaver.ui.data.IValueController;

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
		boolean inline = valueController.getEditType() == IValueController.EditType.INLINE;

		String formaterId = "";
		Object valueHandler = valueController.getValueHandler();
		if (valueHandler instanceof DateTimeCustomValueHandler) {
			DateTimeCustomValueHandler dateTimeValueHandler = (DateTimeCustomValueHandler) valueHandler;
			formaterId = dateTimeValueHandler.getFormatterId(valueController.getValueType());
		}

		timeEditor = new CustomTimeEditor(valueController.getEditPlaceholder(), (inline ? SWT.BORDER : SWT.MULTI),
				formaterId);
		timeEditor.setEditable(!valueController.isReadOnly());
		timeEditor.addSelectionAdapter(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				dirty = true;
			}
		});

		return timeEditor.getControl();
	}

	@Override
	public Object extractEditorValue() throws DBException {
		try (DBCSession session = valueController.getExecutionContext().openSession(new VoidProgressMonitor(),
				DBCExecutionPurpose.UTIL, "Make datetime value from editor")) {
			return valueController.getValueHandler().getValueFromObject(session, valueController.getValueType(),
					timeEditor.getValue(), false);
		}
	}

	@Override
	public void primeEditorValue(@Nullable Object value) throws DBException {
		if (value instanceof Date) {
			timeEditor.setValue((Date) value);
		}
		if (valueController.getEditType() == IValueController.EditType.INLINE) {
			timeEditor.selectAll();
		}
	}

}
