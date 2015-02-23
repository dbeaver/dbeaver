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
/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * ICellValidator to validate direct edit values in the table label. Collaborates
 * with an instance of ValidationMessageHandler
 * @author Serge Rieder
 */
public class TableNameCellEditorValidator implements ICellEditorValidator
{

	private ValidationMessageHandler handler;

	/**
	 * @param validationMessageHandler
	 *            the validation message handler to pass error information to
	 */
	public TableNameCellEditorValidator(ValidationMessageHandler validationMessageHandler)
	{
		this.handler = validationMessageHandler;
	}

	/**
	 * @param validation
	 *            of column type
	 * @return the error message if an error has occurred, otherwise null
	 */
	@Override
    public String isValid(Object value)
	{
		String name = (String) value;

		if (name.indexOf(" ") != -1)
		{
			String text = "Table name should not include the space character";
			return setMessageText(text);
		}

		if (name.length() == 0)
		{
			String text = "Table name should include at least one character";
			return setMessageText(text);
		}

		unsetMessageText();
		return null;

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