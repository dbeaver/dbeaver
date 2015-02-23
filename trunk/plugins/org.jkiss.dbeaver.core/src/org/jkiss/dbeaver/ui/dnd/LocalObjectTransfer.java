/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.dnd;

import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.TransferData;

/**
 * This Transfer will only work when dragging within the same instance of Eclipse.
 * Subclasses should maintain a
 * single instance of their Transfer and provide a static method to obtain that
 * instance.
 */
public abstract class LocalObjectTransfer<OBJECT_TYPE> extends ByteArrayTransfer {

	private OBJECT_TYPE object;
	private long startTime;

	/**
	 * Returns the Object.
	 *
	 * @return The Object
	 */
	public OBJECT_TYPE getObject() {
		return object;
	}

	/**
	 * The data object is not converted to bytes. It is held onto in a field.
	 * Instead, a checksum is written out to prevent unwanted drags across
	 * multiple running copies of Eclipse.
	 *
	 * @see org.eclipse.swt.dnd.Transfer#javaToNative(Object, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
    public void javaToNative(Object object, TransferData transferData) {
		setObject((OBJECT_TYPE)object);
		startTime = System.currentTimeMillis();
		if (transferData != null)
			super.javaToNative(String.valueOf(startTime).getBytes(),
					transferData);
	}

	/**
	 * The data object is not converted to bytes. It is held onto in a field.
	 * Instead, a checksum is written out to prevent unwanted drags across
	 * mulitple running. copies of Eclipse.
	 *
	 * @see org.eclipse.swt.dnd.Transfer#nativeToJava(org.eclipse.swt.dnd.TransferData)
	 */
	@Override
    public Object nativeToJava(TransferData transferData) {
		byte bytes[] = (byte[]) super.nativeToJava(transferData);
		if (bytes == null) {
			return null;
		}
		long startTime = Long.parseLong(new String(bytes));
		return (this.startTime == startTime) ? getObject() : null;
	}

	/**
	 * Sets the Object.
	 * 
	 * @param obj
	 *            The Object
	 */
	public void setObject(OBJECT_TYPE obj) {
		object = obj;
	}

}
