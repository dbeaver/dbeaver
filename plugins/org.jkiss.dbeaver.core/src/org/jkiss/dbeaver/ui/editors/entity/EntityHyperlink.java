/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
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
            return DBUtils.getObjectFullName(reference, DBPEvaluationContext.UI);
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
                node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, object, true);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            } finally {
                monitor.done();
            }
        }
    }
}
