/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls.itemlist;

import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.meta.DBMNode;

import java.util.List;

/**
 * FolderRow
*/
class ItemRow
{
    final DBMNode object;
    final List<ItemCell> props;

    ItemRow(DBMNode object, List<ItemCell> props)
    {
        this.object = object;
        this.props = props;
    }
    Object getValue(int index)
    {
        return index >= props.size() ? null : props.get(index).value;
    }

    public DBPObject getObject()
    {
        return object.getObject();
    }
}