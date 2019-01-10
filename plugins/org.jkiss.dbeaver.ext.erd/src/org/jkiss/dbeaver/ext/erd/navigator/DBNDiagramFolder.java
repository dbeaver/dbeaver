/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.erd.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;

/**
 * DBNDiagramFolder
 */
public class DBNDiagramFolder extends DBNResource
{
    private static final DBIcon FOLDER_ICON = new DBIcon(ERDConstants.ICON_LOCATION_PREFIX + "erd_folder.png");

    private DBPImage image;

    public DBNDiagramFolder(DBNNode parentNode, IResource resource, DBPResourceHandler handler) throws DBException, CoreException
    {
        super(parentNode, resource, handler);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        super.dispose(reflect);
    }

    @Override
    public DBPImage getNodeIcon()
    {
        IResource resource = getResource();
        if (resource != null && resource.getParent() instanceof IProject) {
            if (image == null) {
                image = FOLDER_ICON;
            }
            return image;
        }
        return super.getNodeIcon();
    }

    @Override
    public boolean supportsDrop(DBNNode otherNode)
    {
        return otherNode instanceof DBNDiagram || super.supportsDrop(otherNode);
    }

}
