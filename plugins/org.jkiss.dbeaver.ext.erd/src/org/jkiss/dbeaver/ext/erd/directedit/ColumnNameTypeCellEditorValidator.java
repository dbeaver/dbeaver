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
/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.jface.viewers.ICellEditorValidator;


/**
 * ICellValidator to validate direct edit values in the column label
 * Collaborates with an instance of ValidationMessageHandler
 * @author Serge Rider
 */
public class ColumnNameTypeCellEditorValidator implements ICellEditorValidator
{

	private ValidationMessageHandler handler;
	
	/**
	 * @param validationMessageHandler the validation message handler to pass error information to
	 */
	public ColumnNameTypeCellEditorValidator(ValidationMessageHandler validationMessageHandler)
	{
		this.handler = validationMessageHandler;
	}

	/**
	 * @return the error message if an error has occurred, otherwise null
	 */
	@Override
    public String isValid(Object value)
	{
		String string = (String)value;
		String name = null;
		String type = null;
		int colonIndex = string.indexOf(':');
		if (colonIndex >= 0)
		{
			name = string.substring(0, colonIndex);
			if (string.length() > colonIndex+1)
			{
				type = string.substring(colonIndex+1);
			}
		}
		if (name != null && type!= null)
		{
			
			if (name.indexOf(" ")!= -1)
			{
				String text = "Column name should not include the space character";
				return setMessageText(text);
			} else {
                return unsetMessageText();
            }
		}
		else
		{
			String text = "Invalid format for text entry. Needs [name]:[type] format";
			return setMessageText(text);
		}
	}


	private String unsetMessageText()
	{
		handler.reset();
		return null;
	}

	


	private String setMessageText(String text)
	{
		handler.setMessageText(text);
		return text;
	}



}
