/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 25, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

/**
 * Represents interface for outputting validation error messages to some widget
 * @author Serge Rieder
 */
public interface ValidationMessageHandler
{

	public void setMessageText(String text);

	/**
	 * Resets so that the validation message is no longer shown
	 */
	public void reset();
}