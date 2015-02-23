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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

import java.lang.reflect.InvocationTargetException;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private DBNDatabaseNode node;
    private IRegion region;
    private DBSObjectReference reference;

    public EntityHyperlink(DBNDatabaseNode node, IRegion region)
    {
        this.node = node;
        this.region = region;
    }

    public EntityHyperlink(DBSObjectReference reference, IRegion region)
    {
        this.reference = reference;
        this.region = region;
    }

    @Override
    public IRegion getHyperlinkRegion()
    {
        return region;
    }

    @Override
    public String getTypeLabel()
    {
        return null;
    }

    @Override
    public String getHyperlinkText()
    {
        if (reference != null) {
            return DBUtils.getObjectFullName(reference);
        } else {
            return node.getNodeFullName();
        }
    }

    @Override
    public void open()
    {
        DBNDatabaseNode objectNode;
        if (reference != null) {
            ObjectFinder finder = new ObjectFinder();
            try {
                DBeaverUI.runInProgressService(finder);
            } catch (InterruptedException e) {
                return;
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(
                    null, "Can't find object", "Can't find referenced object in database model", e.getTargetException());
                return;
            }
            objectNode = finder.node;
        } else {
            objectNode = node;
        }
        if (objectNode != null) {
            NavigatorHandlerObjectOpen.openEntityEditor(objectNode, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
        }
    }

    private class ObjectFinder implements DBRRunnableWithProgress {

        private DBNDatabaseNode node;

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            monitor.beginTask("Resolve object " + reference.getName(), 1);
            try {
                DBSObject object = reference.resolveObject(monitor);
                node = DBNModel.getInstance().getNodeByObject(monitor, object, true);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            } finally {
                monitor.done();
            }
        }
    }
}
