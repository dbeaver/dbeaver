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
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerOpenObject;

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
        if (object instanceof DBSEntityQualified) {
            return ((DBSEntityQualified)object).getFullQualifiedName();
        } else {
            return object.getName();
        }
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
                    NavigatorHandlerOpenObject.openEntityEditor(node, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                }
            }
        });
    }
}
