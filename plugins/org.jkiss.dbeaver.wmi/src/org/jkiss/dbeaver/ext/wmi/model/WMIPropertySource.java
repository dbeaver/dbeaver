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
package org.jkiss.dbeaver.ext.wmi.model;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ui.properties.DefaultPropertyLabelProvider;
import org.jkiss.wmi.service.WMIException;
import org.jkiss.wmi.service.WMIQualifiedObject;
import org.jkiss.wmi.service.WMIQualifier;

import java.util.Collection;

/**
 * Property source based on WMI qualifiers
 */
public abstract class WMIPropertySource implements IPropertySource
{
    static final Log log = Log.getLog(WMIPropertySource.class);
    private static final IPropertyDescriptor[] EMPTY_PROPERTIES = new IPropertyDescriptor[0];

    protected abstract WMIQualifiedObject getQualifiedObject();

    protected boolean getFlagQualifier(String qName) throws DBException
    {
        WMIQualifiedObject qualifiedObject = getQualifiedObject();
        try {
            return qualifiedObject != null && Boolean.TRUE.equals(
                qualifiedObject.getQualifier(qName));
        } catch (WMIException e) {
            throw new DBException("Can't extract object qualifiers", e);
        }
    }

    @Override
    public Object getEditableValue()
    {
        return this;//getQualifiedObject();
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors()
    {
        try {
            WMIQualifiedObject qualifiedObject = getQualifiedObject();
            if (qualifiedObject == null) {
                return EMPTY_PROPERTIES;
            }
            Collection<WMIQualifier> qualifiers = qualifiedObject.getQualifiers();
            IPropertyDescriptor[] result = new IPropertyDescriptor[qualifiers.size()];
            int index = 0;
            for (WMIQualifier qualifier : qualifiers) {
                String name = qualifier.getName();
                PropertyDescriptor prop = new PropertyDescriptor(name, name);
                prop.setLabelProvider(DefaultPropertyLabelProvider.INSTANCE);
                prop.setCategory("WMI");
                result[index++] = prop;
            }
            return result;
        } catch (WMIException e) {
            log.error(e);
            return EMPTY_PROPERTIES;
        }
    }

    @Override
    public Object getPropertyValue(Object id)
    {
        try {
            return getQualifiedObject().getQualifier(id.toString());
        } catch (WMIException e) {
            log.error(e);
            return null;
        }
    }

    @Override
    public boolean isPropertySet(Object id)
    {
        try {
            return getQualifiedObject().getQualifier(id.toString()) != null;
        } catch (WMIException e) {
            log.error(e);
            return false;
        }
    }

    @Override
    public void resetPropertyValue(Object id)
    {

    }

    @Override
    public void setPropertyValue(Object id, Object value)
    {

    }

}
