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

/**
 * QM meta event
 */
public class QMMetaEvent {

    public enum Action {
        BEGIN,
        END,
        UPDATE,
    }

    private final QMMObject object;
    private final Action action;

    public QMMetaEvent(QMMObject object, Action action)
    {
        this.object = object;
        this.action = action;
    }

    public QMMObject getObject()
    {
        return object;
    }

    public Action getAction()
    {
        return action;
    }

    @Override
    public String toString()
    {
        return action + " " + object;
    }
}
