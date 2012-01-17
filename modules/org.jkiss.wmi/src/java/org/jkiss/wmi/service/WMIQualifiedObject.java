/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
