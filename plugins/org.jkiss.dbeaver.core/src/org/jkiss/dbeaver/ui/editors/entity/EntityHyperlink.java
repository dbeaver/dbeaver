/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private DBNDatabaseNode node;
    private IRegion region;

    public EntityHyperlink(DBNDatabaseNode node, IRegion region)
    {
        this.node = node;
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
        return node.getNodePathName();
    }

    @Override
    public void open()
    {
        NavigatorHandlerObjectOpen.openEntityEditor(node, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }

}
