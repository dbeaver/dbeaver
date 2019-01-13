/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingElement;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;

import java.util.Collections;
import java.util.List;

/**
 * Complex value element.
 * Map pair or array item
 */
public class CollectionElementData {

    private static final Log log = Log.getLog(CollectionElementData.class);

    final DBDAttributeBinding collectionBinding;
    final DBDAttributeBindingElement[] elements;

    public CollectionElementData(DBDAttributeBinding collectionBinding, DBDCollection collection) {
        this.collectionBinding = collectionBinding;

        int count = collection.getItemCount();
        elements = new DBDAttributeBindingElement[count];
        for (int i = 0; i < count; i++) {
            elements[i] = new DBDAttributeBindingElement(collectionBinding, collection, i);
        }
        try (DBCSession session = DBUtils.openMetaSession(new VoidProgressMonitor(), collectionBinding, "Collection types read")) {
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
        }
    }

    public DBDAttributeBinding getCollectionBinding() {
        return collectionBinding;
    }

    public DBDAttributeBindingElement[] getElements() {
        return elements;
    }
}
