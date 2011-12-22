/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.project;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Default resource handler
 */
public class DefaultResourceHandlerImpl extends AbstractResourceHandler {

    //static final Log log = LogFactory.getLog(DefaultResourceHandlerImpl.class);

    public static final DefaultResourceHandlerImpl INSTANCE = new DefaultResourceHandlerImpl();

    private static class ProgramInfo {
        final Program program;
        Image image;

        private ProgramInfo(Program program)
        {
            this.program = program;
        }
    }

    private static final Map<String, ProgramInfo> programMap = new HashMap<String, ProgramInfo>();

    private static ProgramInfo getProgram(IResource resource)
    {
        if (resource instanceof IFile) {
            final String fileExtension = CommonUtils.getString(resource.getFileExtension());
            ProgramInfo programInfo = programMap.get(fileExtension);
            if (programInfo == null) {
                Program program = Program.findProgram(fileExtension);
                programInfo = new ProgramInfo(program);
                if (program != null) {
                    final ImageData imageData = program.getImageData();
                    if (imageData != null) {
                        programInfo.image = new Image(DBeaverCore.getDisplay(), imageData);
                    }
                }
                programMap.put(fileExtension, programInfo);
            }
            return programInfo.program == null ? null : programInfo;
        }
        return null;
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFile) {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
        return super.getFeatures(resource);
    }

    public String getTypeName(IResource resource)
    {
        final ProgramInfo program = getProgram(resource);
        if (program != null) {
            return program.program.getName();
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
        ProgramInfo program = getProgram(resource);
        if (program != null && program.image != null) {
            node.setResourceImage(program.image);
        }
        return node;
    }

    @Override
    public void openResource(IResource resource, IWorkbenchWindow window) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            Program.launch(resource.getLocation().toFile().getAbsolutePath());
        }
    }

}
