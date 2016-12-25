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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * TreeLoadService
 */
public class TreeLoadService extends DatabaseLoadService<Object[]> {

    private DBNNode parentNode;

    public TreeLoadService(String serviceName, DBNDatabaseNode parentNode)
    {
        super(serviceName, parentNode);
        this.parentNode = parentNode;
    }

    public DBNNode getParentNode() {
        return parentNode;
    }

    @Override
    public Object[] evaluate(DBRProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException
    {
        try {
            DBNNode[] children = NavigatorUtils.getNodeChildrenFiltered(monitor, parentNode, true);
            return children == null ? new Object[0] : children;
        } catch (Throwable ex) {
            throw new InvocationTargetException(ex);
        }
    }

}
