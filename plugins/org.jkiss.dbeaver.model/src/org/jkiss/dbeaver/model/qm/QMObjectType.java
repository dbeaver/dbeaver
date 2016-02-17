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
package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.qm.meta.QMMObject;
import org.jkiss.dbeaver.model.qm.meta.QMMSessionInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMStatementInfo;
import org.jkiss.dbeaver.model.qm.meta.QMMTransactionInfo;
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
        List<String> names = new ArrayList<>(objectTypes.size());
        for (QMObjectType type : objectTypes) {
            names.add(type.name());
        }
        return CommonUtils.makeString(names, ',');
    }

    public static Collection<QMObjectType> fromString(String str)
    {
        List<QMObjectType> objectTypes = new ArrayList<>();
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
