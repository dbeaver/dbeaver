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
package org.jkiss.dbeaver.tasks.ui.view;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.ui.dnd.LocalObjectTransfer;

import java.util.List;

/**
 * Used to move DBTTask around
 */
public final class DatabaseTaskTransfer extends LocalObjectTransfer<List<DBTTask>> {

	public static class Data {
		private Control sourceControl;
		private List<DBTTask> tasks;

		public Data(Control sourceControl, List<DBTTask> tasks) {
			this.sourceControl = sourceControl;
			this.tasks = tasks;
		}

		public Control getSourceControl() {
			return sourceControl;
		}

		public List<DBTTask> getTasks() {
			return tasks;
		}
	}

	private static final DatabaseTaskTransfer INSTANCE = new DatabaseTaskTransfer();
	private static final String TYPE_NAME = "DBTTask Transfer"//$NON-NLS-1$
			+ System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
	private static final int TYPEID = registerType(TYPE_NAME);

	public static DatabaseTaskTransfer getInstance() {
		return INSTANCE;
	}

	private DatabaseTaskTransfer() {
	}

	@Override
    protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	@Override
    protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

    public static List<DBTTask> getFromClipboard()
    {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        try {
            return (List<DBTTask>) clipboard.getContents(DatabaseTaskTransfer.getInstance());
        } finally {
            clipboard.dispose();
        }
    }

}
