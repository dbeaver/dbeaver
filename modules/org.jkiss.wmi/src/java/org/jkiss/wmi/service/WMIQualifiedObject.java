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

package org.jkiss.wmi.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Qualified object
 */
public abstract class WMIQualifiedObject {

    private volatile List<WMIQualifier> qualifiers;

    public Collection<WMIQualifier> getQualifiers()
        throws WMIException
    {
        if (qualifiers == null) {
            synchronized (this) {
                if (qualifiers == null) {
                    qualifiers = new ArrayList<WMIQualifier>();
                    readObjectQualifiers(qualifiers);
                }
            }
        }
        return qualifiers;
    }

    public Object getQualifier(String name)
        throws WMIException
    {
        for (WMIQualifier q : getQualifiers()) {
            if (q.getName().equalsIgnoreCase(name)) {
                return q.getValue();
            }
        }
        return null;
    }

    protected abstract void readObjectQualifiers(List<WMIQualifier> qualifiers) throws WMIException;

}
