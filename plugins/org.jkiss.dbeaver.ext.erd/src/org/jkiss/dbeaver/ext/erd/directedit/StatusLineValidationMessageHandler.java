/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
/*
 * Created on Aug 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.ui.IEditorSite;


/**
 * Outputs validation messages to status line
 * @author Serge Rider
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