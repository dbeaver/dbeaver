/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.wmi.service;

/**
 * WMI object property
 */
public class WMIObjectMethod extends WMIObjectElement
{

    private WMIObject inParameter;
    private WMIObject outParameter;

    public WMIObjectMethod(WMIObject owner, String name, WMIObject inParameter, WMIObject outParameter)
    {
        super(owner, name);
        this.inParameter = inParameter;
        this.outParameter = outParameter;
    }

    public WMIObject getInParameter()
    {
        return inParameter;
    }

    public WMIObject getOutParameter()
    {
        return outParameter;
    }

    @Override
    public String toString()
    {
        return getName() + "()";
    }
}
