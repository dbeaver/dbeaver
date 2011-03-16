/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dnd;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.Collection;

/**
 * Used to move DBSObject around in a database navigator.
 */
public final class DatabaseObjectTransfer extends LocalObjectTransfer<Collection<DBSObject>> {

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
	protected int[] getTypeIds() {
		return new int[] { TYPEID };
	}

	/**
	 * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
	 */
	protected String[] getTypeNames() {
		return new String[] { TYPE_NAME };
	}

    public static Collection<DBSObject> getFromClipboard()
    {
        Clipboard clipboard = new Clipboard(Display.getDefault());
        return (Collection<DBSObject>) clipboard.getContents(DatabaseObjectTransfer.getInstance());
    }

}
