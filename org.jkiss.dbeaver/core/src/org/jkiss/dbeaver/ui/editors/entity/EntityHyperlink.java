/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.model.meta.DBMDataSource;
import org.jkiss.dbeaver.model.meta.DBMNode;
import org.jkiss.dbeaver.model.meta.DBMTreeFolder;
import org.jkiss.dbeaver.ui.actions.OpenObjectEditorAction;

/**
 * EntityHyperlink
 */
public class EntityHyperlink implements IHyperlink
{
    private DBMNode node;
    private IRegion region;

    public EntityHyperlink(DBMNode node, IRegion region)
    {
        this.node = node;
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
        for (DBMNode t = node; t != null; t = t.getParentNode()) {
            if (t instanceof DBMTreeFolder) {
                continue;
            }
            if (t instanceof DBMDataSource) {
                break;
            }
            if (node.getParentNode() != null) {
                nodeFullName.insert(0, '.');
            }
            nodeFullName.insert(0, t.getNodeName());
        }
        return nodeFullName.toString();
    }

    public void open()
    {
        OpenObjectEditorAction.openEntityEditor(node, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
    }
}
