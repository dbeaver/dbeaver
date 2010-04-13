/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.dbeaver.ui.views.properties.PropertyAnnoDescriptor;

/**
 * FolderCell
*/
class ItemCell
{
    final PropertyAnnoDescriptor prop;
    Object value;

    ItemCell(PropertyAnnoDescriptor prop, Object value)
    {
        this.prop = prop;
        this.value = value;
    }
}