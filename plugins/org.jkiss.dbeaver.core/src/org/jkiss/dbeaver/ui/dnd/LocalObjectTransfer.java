/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
