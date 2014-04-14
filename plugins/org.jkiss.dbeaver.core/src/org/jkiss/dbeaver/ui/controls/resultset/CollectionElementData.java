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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingElement;
import org.jkiss.dbeaver.model.data.DBDCollection;

/**
 * Complex value element.
 * Map pair or array item
 */
class CollectionElementData {

    final DBDAttributeBinding collectionBinding;
    final DBDAttributeBindingElement[] elements;

    public CollectionElementData(DBDAttributeBinding collectionBinding, DBDCollection collection) {
        this.collectionBinding = collectionBinding;

        int count = collection.getItemCount();
        elements = new DBDAttributeBindingElement[count];
        for (int i = 0; i < count; i++) {
            elements[i] = new DBDAttributeBindingElement(collectionBinding, collection, i);
        }
    }

}
