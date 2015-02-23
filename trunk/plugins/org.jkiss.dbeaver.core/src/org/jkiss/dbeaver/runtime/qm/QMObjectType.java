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
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;
import org.jkiss.dbeaver.runtime.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementInfo;
import org.jkiss.dbeaver.runtime.qm.meta.QMMTransactionInfo;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object type
 */
public enum QMObjectType {

    session(QMMSessionInfo.class),
    txn(QMMTransactionInfo.class),
    query(QMMStatementInfo.class);

    private final Class<? extends QMMObject> type;

    private QMObjectType(Class<? extends QMMObject> type)
    {
        this.type = type;
    }

    public Class<? extends QMMObject> getType()
    {
        return type;
    }

    public static String toString(Collection<QMObjectType> objectTypes)
    {
        List<String> names = new ArrayList<String>(objectTypes.size());
        for (QMObjectType type : objectTypes) {
            names.add(type.name());
        }
        return CommonUtils.makeString(names, ',');
    }

    public static Collection<QMObjectType> fromString(String str)
    {
        List<QMObjectType> objectTypes = new ArrayList<QMObjectType>();
        for (String otName : CommonUtils.splitString(str, ',')) {
            try {
                objectTypes.add(QMObjectType.valueOf(otName));
            } catch (IllegalArgumentException e) {
                // just scrip bad names
            }
        }
        return objectTypes;
    }
}
