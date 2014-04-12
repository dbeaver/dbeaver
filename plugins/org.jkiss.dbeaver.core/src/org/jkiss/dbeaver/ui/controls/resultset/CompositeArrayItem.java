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

import org.eclipse.swt.graphics.Image;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDCollection;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCException;

/**
 * Collection item
 */
public class CompositeArrayItem implements CompositeObjectElement {

    @NotNull
    private final DBDCollection ownerValue;
    private final int index;

    public CompositeArrayItem(@NotNull DBDCollection ownerValue, int index) {
        this.ownerValue = ownerValue;
        this.index = index;
    }

    @NotNull
    public String getLabel() {
        return String.valueOf(index);
    }

    @NotNull
    public Image getImage() {
        return DBUtils.getDataIcon(ownerValue.getComponentType()).getImage();
    }

    @NotNull
    public DBDValueHandler getValueHandler() {
        return ownerValue.getComponentValueHandler();
    }

    @Nullable
    public Object getValue() {
        return ownerValue.getItem(index);
    }

    public void setValue(Object value) throws DBCException {
        ownerValue.setItem(index, value);
    }

    @Override
    public String toString() {
        return getLabel() + "=" + getValue();
    }
}
