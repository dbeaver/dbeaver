/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 19, 2004
 */
package org.jkiss.dbeaver.ext.erd.directedit;

import org.eclipse.jface.viewers.ICellEditorValidator;

/**
 * ICellValidator to validate direct edit values in the table label. Collaborates
 * with an instance of ValidationMessageHandler
 * @author Phil Zoio
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