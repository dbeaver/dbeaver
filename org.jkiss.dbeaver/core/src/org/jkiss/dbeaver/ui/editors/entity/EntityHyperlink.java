/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorOpenObjectHandler;

import java.lang.reflect.InvocationTargetException;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private DBSObject object;
    private IRegion region;

    public EntityHyperlink(DBSObject object, IRegion region)
    {
        this.object = object;
        this.region = region;
    }

    public IRegion getHyperlinkRegion()
    {
        return region;
    }

    public String getTypeLabel()
    {
        return null;
    }

    public String getHyperlinkText()
    {
        StringBuilder nodeFullName = new StringBuilder();
        for (DBSObject t = object; t != null; t = t.getParentObject()) {
            if (t instanceof DBNTreeFolder) {
                continue;
            }
            if (t instanceof DBNDataSource) {
                break;
            }
            if (object.getParentObject() != null) {
                nodeFullName.insert(0, '.');
            }
            nodeFullName.insert(0, t.getName());
        }
        return nodeFullName.toString();
    }

    public void open()
    {
        DBeaverCore.runUIJob("Open hyperlink", new DBRRunnableWithProgress() {
            public void run(DBRProgressMonitor monitor)
                throws InvocationTargetException, InterruptedException
            {
                DBNNode node = DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(
                    monitor,
                    object,
                    true
                );
                if (node != null) {
                    NavigatorOpenObjectHandler.openEntityEditor(node, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                }
            }
        });
    }
}
