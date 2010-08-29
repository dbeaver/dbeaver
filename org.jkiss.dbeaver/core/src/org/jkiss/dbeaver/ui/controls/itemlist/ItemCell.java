/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNNode;

/**
 * FolderCell
*/
class ItemCell
{
    final DBNNode node;
    final PropertyAnnoDescriptor prop;
    Object value;

    ItemCell(DBNNode node, PropertyAnnoDescriptor prop, Object value)
    {
        this.node = node;
        this.prop = prop;
        this.value = value;
    }
}