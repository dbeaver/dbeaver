/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic entity identifier
 */
public class BaseEntityIdentifier implements DBCEntityIdentifier {

    private DBSEntityReferrer referrer;

    private final List<DBDAttributeBinding> attributes = new ArrayList<DBDAttributeBinding>();

    public BaseEntityIdentifier(DBRProgressMonitor monitor, DBSEntityReferrer referrer, DBDAttributeBinding[] bindings) throws DBException
    {
        this.referrer = referrer;
        reloadAttributes(monitor, bindings);
    }

    public void reloadAttributes(DBRProgressMonitor monitor, DBDAttributeBinding[] bindings) throws DBException
    {
        this.attributes.clear();
        for (DBSEntityAttributeRef cColumn : CommonUtils.safeCollection(referrer.getAttributeReferences(monitor))) {
            for (DBDAttributeBinding binding : bindings) {
                if (binding.matches(cColumn.getAttribute())) {
                    this.attributes.add(binding);
                }
            }
        }
    }

    public DBSEntityReferrer getReferrer()
    {
        return referrer;
    }

    @Override
    public List<DBDAttributeBinding> getAttributes() {
        return attributes;
    }
}