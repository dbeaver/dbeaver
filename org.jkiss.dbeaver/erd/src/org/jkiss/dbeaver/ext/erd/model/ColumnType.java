/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Represents our allowable data types - a small subset of those available in a real database!
 * @author Phil Zoio
 */
public class ColumnType
{

	private static List types = new ArrayList();
	public static ColumnType VARCHAR = new ColumnType("VARCHAR");
	public static ColumnType INTEGER = new ColumnType("INTEGER");
	public static ColumnType DATE = new ColumnType("DATE");

	private String type;

	private ColumnType(String type)
	{
		this.type = type;
		String typeToAdd = this.getType();
		types.add(typeToAdd);
	}

	/**
	 * @return Returns the type.
	 */
	public String getType()
	{
		return type;
	}

	public static boolean hasType(String type)
	{
		return types.contains(type.toUpperCase());
	}

	public static String getTypes()
	{
		StringBuffer typeBuffer = new StringBuffer();
		for (Iterator iter = types.iterator(); iter.hasNext();)
		{
			String element = (String) iter.next();
			typeBuffer.append(element).append(", ");
		}
		if (types.size() >= 1)
		{
			typeBuffer.delete(typeBuffer.length() - 2, typeBuffer.length());
		}
		return typeBuffer.toString();
	}

}