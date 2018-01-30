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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.DateTimeStandaloneEditor;

import java.util.Date;

/**
 * JDBC string value handler
 */
public class DateTimeValueManager extends BaseValueManager {

	protected static final Log log = Log.getLog(DateTimeValueManager.class);

	@Override
	public void contributeActions(@NotNull IContributionManager manager, @NotNull final IValueController controller,
			@Nullable IValueEditor activeEditor) throws DBCException {
		super.contributeActions(manager, controller, activeEditor);
		manager.add(new Action(CoreMessages.model_jdbc_set_to_current_time,
				DBeaverIcons.getImageDescriptor(DBIcon.TYPE_DATETIME)) {
			@Override
			public void run() {
				controller.updateValue(new Date(), true);
			}
		});
	}

	@NotNull
	@Override
	public IValueController.EditType[] getSupportedEditTypes() {
		return new IValueController.EditType[] { IValueController.EditType.INLINE, IValueController.EditType.PANEL,
				IValueController.EditType.EDITOR };
	}

	@Override
	public IValueEditor createEditor(@NotNull IValueController controller) throws DBException {

		boolean isUseDateTimeEditor = controller.getExecutionContext().getDataSource().getContainer()
				.getPreferenceStore().getBoolean(DBeaverPreferences.RESULT_SET_DATETIME_USE_CONTENT_EDITOR);
		switch (controller.getEditType()) {
		case INLINE:
		case PANEL:
			return new DateTimeInlineEditor(controller, isUseDateTimeEditor);
		case EDITOR:
			return new DateTimeStandaloneEditor(controller);
		default:
			return null;
		}
	}

}