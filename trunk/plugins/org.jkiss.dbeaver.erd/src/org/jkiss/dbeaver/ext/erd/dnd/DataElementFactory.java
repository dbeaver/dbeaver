/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
/*
 * Created on Jul 14, 2004
 */
package org.jkiss.dbeaver.ext.erd.dnd;

import org.eclipse.gef.requests.CreationFactory;

/**
 * Factory for creating instances of new objects from a palette
 * @author Serge Rieder
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
	@Override
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
	@Override
    public Object getObjectType()
	{
		return template;
	}

}