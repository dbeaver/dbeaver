/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Aug 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.ui.IEditorSite;


/**
 * Outputs validation messages to status line
 * @author Phil Zoio
 */
public class StatusLineValidationMessageHandler implements ValidationMessageHandler
{

	private IEditorSite editorSite;

	/**
	 * @param editorSite
	 */
	public StatusLineValidationMessageHandler(IEditorSite editorSite)
	{
		this.editorSite = editorSite;
	}

	/**
	 * Sets the status message
	 * 
	 * @param text
	 *            the message to display
	 */
	public void setMessageText(String text)
	{
		editorSite.getActionBars().getStatusLineManager().setErrorMessage(text);
	}

	/**
	 * Sets clears the status line
	 */
	public void reset()
	{
		editorSite.getActionBars().getStatusLineManager().setErrorMessage(null);
	}

}