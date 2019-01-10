/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.navigator.DBNNode;

import java.util.Collection;

/**
 * Used to move DBNNode around in a database navigator.
 */
public final class TreeNodeTransfer extends LocalObjectTransfer<Collection<DBNNode>> {

	private static final TreeNodeTransfer INSTANCE = new TreeNodeTransfer();
	private static final String TYPE_NAME = "DBNNode Transfer"//$NON-NLS-1$
			+ System.currentTimeMillis() + ":" + INSTANCE.hashCode();//$NON-NLS-1$
	private static final int TYPEID = registerType(TYPE_NAME);

	/**
	 * Returns the singleton instance.
	 *
	 * @return The singleton instance
	 */
	public static TreeNodeTransfer getInstance() {
		return INSTANCE;
	}

	private TreeNodeTransfer() {
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

    public static Collection<DBNNode> getFromClipboard()
    {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        try {
            return (Collection<DBNNode>) clipboard.getContents(TreeNodeTransfer.getInstance());
        } finally {
            clipboard.dispose();
        }
    }

}
