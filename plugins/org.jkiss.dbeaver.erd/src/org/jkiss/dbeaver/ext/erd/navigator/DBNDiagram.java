/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.Activator;
import org.jkiss.dbeaver.ext.erd.model.EntityDiagram;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * DBNDiagram
 */
public class DBNDiagram extends DBNResource
{
    private Image image;
    private EntityDiagram diagram;

    public DBNDiagram(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
    }

    protected void dispose(boolean reflect)
    {
        UIUtils.dispose(image);
        super.dispose(reflect);
    }

    public Image getNodeIcon()
    {

        if (image == null) {
            image = Activator.getImageDescriptor("icons/erd.png").createImage();
        }
        return image;
    }

    public EntityDiagram getDiagram()
    {
        return diagram;
    }
}
