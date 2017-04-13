/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.resources;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.ProgramInfo;

/**
 * Default resource handler
 */
public class DefaultResourceHandlerImpl extends AbstractResourceHandler {

    public static final String DEFAULT_ROOT = "Resources"; //$NON-NLS-1$
    public static final DefaultResourceHandlerImpl INSTANCE = new DefaultResourceHandlerImpl();

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    @Override
    public String getTypeName(IResource resource)
    {
        final ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null) {
            return program.getProgram().getName();
        }
        return "resource"; //$NON-NLS-1$
    }

    @Override
    public String getResourceDescription(IResource resource)
    {
        return "";
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null && program.getImage() != null) {
            node.setResourceImage(program.getImage());
        }
        return node;
    }

    @Override
    public void openResource(IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            RuntimeUtils.launchProgram(resource.getLocation().toFile().getAbsolutePath());
        }
    }

}
