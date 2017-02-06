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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.IWorkbenchSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNodeHandler;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private IWorkbenchSite site;
    private IRegion region;
    private DBSObjectReference reference;

    public EntityHyperlink(IWorkbenchSite site, DBSObjectReference reference, IRegion region)
    {
        this.site = site;
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
        return DBUtils.getObjectFullName(reference, DBPEvaluationContext.UI);
    }

    @Override
    public void open()
    {
        new ObjectFinder().schedule();
    }

    private class ObjectFinder extends AbstractJob {

        private DBNDatabaseNode node;

        protected ObjectFinder() {
            super("Find object node by reference");
        }

        @Override
        public IStatus run(DBRProgressMonitor monitor)
        {
            monitor.beginTask("Resolve object " + reference.getName(), 1);
            try {
                DBSObject object = reference.resolveObject(monitor);
                node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(monitor, object, true);

                if (node != null) {
                    DBeaverUI.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            NavigatorUtils.executeNodeAction(DBXTreeNodeHandler.Action.open, node, site);
                        }
                    });
                }

            } catch (DBException e) {
                return GeneralUtils.makeExceptionStatus(e);
            } finally {
                monitor.done();
            }
            return Status.OK_STATUS;
        }
    }
}
