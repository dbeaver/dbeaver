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
package org.jkiss.dbeaver.ui.dnd;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPNamedObject;

import java.util.Collection;

/**
 * Used to move DBSObject around in a database navigator.
 */
public final class DatabaseObjectTransfer extends LocalObjectTransfer<Collection<DBPNamedObject>> {

	private static final DatabaseObjectTransfer INSTANCE = new DatabaseObjectTransfer();
	private static final String TYPE_NAME = "DBSObject Transfer"//$NON-NLS-1$
			+ System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
	private static final int TYPEID = registerType(TYPE_NAME);

	/**
	 * Returns the singleton instance.
	 *
	 * @return The singleton instance
	 */
	public static DatabaseObjectTransfer getInstance() {
		return INSTANCE;
	}

	private DatabaseObjectTransfer() {
	}

	/**
	 * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
	 */
	@Override
    protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	/**
	 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
	 */
	@Override
    protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

    public static Collection<DBPNamedObject> getFromClipboard()
    {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        try {
            return (Collection<DBPNamedObject>) clipboard.getContents(DatabaseObjectTransfer.getInstance());
        } finally {
            clipboard.dispose();
        }
    }

}
