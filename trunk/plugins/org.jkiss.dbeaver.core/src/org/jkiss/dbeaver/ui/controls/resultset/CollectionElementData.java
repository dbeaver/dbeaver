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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingElement;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Complex value element.
 * Map pair or array item
 */
public class CollectionElementData {

    static final Log log = Log.getLog(CollectionElementData.class);

    final DBDAttributeBinding collectionBinding;
    final DBDAttributeBindingElement[] elements;

    public CollectionElementData(DBDAttributeBinding collectionBinding, DBDCollection collection) {
        this.collectionBinding = collectionBinding;

        int count = collection.getItemCount();
        elements = new DBDAttributeBindingElement[count];
        for (int i = 0; i < count; i++) {
            elements[i] = new DBDAttributeBindingElement(collectionBinding, collection, i);
        }
        DBCSession session = collectionBinding.getDataSource().openSession(VoidProgressMonitor.INSTANCE, DBCExecutionPurpose.META, "Collection types read");
        try {
            Object[] row = new Object[1];
            List<Object[]> rows = Collections.singletonList(row);
            for (int i = 0; i < count; i++) {
                row[0] = collection.getItem(i);
                try {
                    elements[i].lateBinding(session, rows);
                } catch (DBException e) {
                    log.error("Error binding collection element", e);
                }
            }
        } finally {
            session.close();
        }
    }

    public DBDAttributeBinding getCollectionBinding() {
        return collectionBinding;
    }

    public DBDAttributeBindingElement[] getElements() {
        return elements;
    }
}
