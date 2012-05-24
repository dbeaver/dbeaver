/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Aug 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.ui.IEditorSite;


/**
 * Outputs validation messages to status line
 * @author Serge Rieder
 */
public class StatusLineValidationMessageHandler implements ValidationMessageHandler
{

	private IEditorSite editorSite;

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
	@Override
    public void setMessageText(String text)
	{
		editorSite.getActionBars().getStatusLineManager().setErrorMessage(text);
	}

	/**
	 * Sets clears the status line
	 */
	@Override
    public void reset()
	{
		editorSite.getActionBars().getStatusLineManager().setErrorMessage(null);
	}

}