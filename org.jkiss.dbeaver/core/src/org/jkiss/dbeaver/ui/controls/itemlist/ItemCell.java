/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;
import org.jkiss.dbeaver.model.meta.DBMNode;

/**
 * FolderCell
*/
class ItemCell
{
    final DBMNode node;
    final PropertyAnnoDescriptor prop;
    Object value;

    ItemCell(DBMNode node, PropertyAnnoDescriptor prop, Object value)
    {
        this.node = node;
        this.prop = prop;
        this.value = value;
    }
}