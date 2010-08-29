/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.requests.CreationFactory;

/**
 * Factory for creating instances of new objects from a palette
 * @author Phil Zoio
 */
public class DataElementFactory implements CreationFactory
{

	private Object template;

	/**
	 * Creates a new FlowElementFactory with the given template object
	 * 
	 * @param o
	 *            the template
	 */
	public DataElementFactory(Object o)
	{
		template = o;
	}

	/**
	 * @see org.eclipse.gef.requests.CreationFactory#getNewObject()
	 */
	public Object getNewObject()
	{
		try
		{
			return ((Class<?>) template).newInstance();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * @see org.eclipse.gef.requests.CreationFactory#getObjectType()
	 */
	public Object getObjectType()
	{
		return template;
	}

}