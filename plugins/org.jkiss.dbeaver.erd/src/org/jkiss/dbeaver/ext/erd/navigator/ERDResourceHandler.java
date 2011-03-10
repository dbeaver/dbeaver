/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.AbstractResourceHandler;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.DBNBookmark;
import org.jkiss.dbeaver.model.navigator.DBNBookmarkFolder;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * Bookmarks handler
 */
public class ERDResourceHandler extends AbstractResourceHandler {

    private static final String ERD_DIR = "Diagrams";
    private static final String ERD_EXT = "erd"; //$NON-NLS-1$

    public static final String RES_TYPE_DIAGRAMS = "erd"; //$NON-NLS-1$

    public static IFolder getDiagramsFolder(IProject project)
    {
        return project.getFolder(ERD_DIR);
    }

    @Override
    public int getFeatures(IResource resource)
    {
        if (resource instanceof IFolder) {
            if (resource.getParent() instanceof IFolder) {
                return FEATURE_DELETE | FEATURE_RENAME | FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
            }
            return FEATURE_CREATE_FOLDER | FEATURE_MOVE_INTO;
        } else {
            return FEATURE_OPEN | FEATURE_DELETE | FEATURE_RENAME;
        }
    }

    public String getTypeName(IResource resource)
    {
        if (resource instanceof IFolder) {
            return "diagram folder";
        } else {
            return "diagram";
        }
    }

    @Override
    public void initializeProject(IProject project, IProgressMonitor monitor) throws CoreException, DBException
    {
        final IFolder diagramsFolder = getDiagramsFolder(project);
        if (!diagramsFolder.exists()) {
            diagramsFolder.create(true, true, monitor);
        }
        diagramsFolder.setPersistentProperty(PROP_RESOURCE_TYPE, RES_TYPE_DIAGRAMS);
    }

    @Override
    public DBNResource makeNavigatorNode(DBNNode parentNode, IResource resource) throws CoreException, DBException
    {
        if (resource instanceof IFile) {
            return new DBNDiagram(parentNode, resource, this);
        } else {
            return new DBNDiagramFolder(parentNode, resource, this);
        }
    }

    @Override
    public void openResource(final IResource resource, final IWorkbenchWindow window) throws CoreException, DBException
    {
        if (!(resource instanceof IFile)) {
            return;
        }
/*
        final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(resource.getProject());
        if (projectNode == null) {
            throw new DBException("Can't find project node for '" + resource.getProject().getName() + "'");
        }
        final BookmarkStorage storage = new BookmarkStorage((IFile) resource, false);
        try {
            final DataSourceDescriptor dataSourceContainer = projectNode.getDatabases().getDataSourceRegistry().getDataSource(storage.getDataSourceId());
            if (dataSourceContainer == null) {
                throw new DBException("Can't find datasource '" + storage.getDataSourceId() + "'");
            }
            //if (!dataSourceContainer.isConnected()) {
            //    dataSourceContainer.connect();
            //}
            final DBNDataSource dsNode = (DBNDataSource)DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(dataSourceContainer);
            dsNode.initializeNode(new Runnable() {
                public void run()
                {
                    if (dsNode.getDataSourceContainer().isConnected()) {
                        Display.getDefault().syncExec(new Runnable() {
                            public void run()
                            {
                                openNodeByPath(dsNode, (IFile) resource, storage, window);
                            }
                        });
                    }
                }
            });
        }
        finally {
            storage.dispose();
        }
*/
    }

    public static void createDiagram(DBNDiagram copyFrom, String title, IFolder folder) throws DBException
    {
        if (folder == null) {
            folder = getDiagramsFolder(copyFrom != null ? copyFrom.getResource().getProject() : DBeaverCore.getInstance().getProjectRegistry().getActiveProject());
        }
        if (folder == null) {
            throw new DBException("Can't detect folder for diagram");
        }

        IFile file = ContentUtils.getUniqueFile(folder, CommonUtils.escapeFileName(title), ERD_EXT);

        updateDiagram(copyFrom, title, file);
    }

    private static void updateDiagram(DBNDiagram copyFrom, String title, IFile file)
        throws DBException
    {
/*
        if (CommonUtils.isEmpty(title)) {
            title = node.getNodeName();
        }

        List<String> nodePath = new ArrayList<String>();
        for (DBNNode parent = node; !(parent instanceof DBNDataSource); parent = parent.getParentNode()) {
            nodePath.add(0, parent.getNodeName());
        }

        BookmarkStorage storage = new BookmarkStorage(
            title,
            node.getNodeType() + " " + node.getNodeName(),
            node.getNodeIconDefault(),
            node.getObject().getDataSource().getContainer().getId(),
            nodePath);

        try {
            InputStream data = storage.serialize();
            file.create(data, true, VoidProgressMonitor.INSTANCE.getNestedMonitor());
        } catch (Exception e) {
            throw new DBException(e);
        }
*/
    }

}
