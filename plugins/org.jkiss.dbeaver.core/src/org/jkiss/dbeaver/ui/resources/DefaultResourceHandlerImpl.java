/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIUtils;
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

    @NotNull
    @Override
    public String getTypeName(@NotNull IResource resource)
    {
        final ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null) {
            return program.getProgram().getName();
        }
        return "resource"; //$NON-NLS-1$
    }

    @Override
    public String getResourceDescription(@NotNull IResource resource)
    {
        return "";
    }

    @NotNull
    @Override
    public DBNResource makeNavigatorNode(@NotNull DBNNode parentNode, @NotNull IResource resource) throws CoreException, DBException
    {
        DBNResource node = super.makeNavigatorNode(parentNode, resource);
        ProgramInfo program = ProgramInfo.getProgram(resource);
        if (program != null && program.getImage() != null) {
            node.setResourceImage(program.getImage());
        }
        return node;
    }

    @Override
    public void openResource(@NotNull IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            IEditorDescriptor desc = PlatformUI.getWorkbench().
                getEditorRegistry().getDefaultEditor(resource.getName());
            if (desc != null) {
                DBeaverUI.getActiveWorkbenchWindow().getActivePage().openEditor(
                    new FileEditorInput((IFile) resource),
                    desc.getId());
            } else {
                UIUtils.launchProgram(resource.getLocation().toFile().getAbsolutePath());
            }
        }
    }

}
