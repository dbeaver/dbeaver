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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorDescriptor;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.ui.IProgressControlProvider;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.ISearchContextProvider;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.*;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorContributorUser;
import org.jkiss.dbeaver.ui.editors.entity.GlobalContributorManager;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjectPropertiesEditor
 */
public class ObjectPropertiesEditor extends AbstractDatabaseObjectEditor<DBSObject>
    implements IRefreshablePart, IProgressControlProvider, IFolderContainer, ISearchContextProvider, INavigatorModelView
{
    private static final Log log = Log.getLog(ObjectPropertiesEditor.class);

    private FolderComposite folderComposite;
    private ObjectEditorPageControl pageControl;
    private final List<IFolderListener> folderListeners = new ArrayList<>();
    private String curFolderId;

    private final List<ISaveablePart> nestedSaveable = new ArrayList<>();
    private final Map<IFolder, IEditorActionBarContributor> pageContributors = new HashMap<>();

    public ObjectPropertiesEditor()
    {
    }

    @Override
    public void createPartControl(Composite parent)
    {
        // Add lazy props listener
        //PropertiesContributor.getInstance().addLazyListener(this);

        pageControl = new ObjectEditorPageControl(parent, SWT.SHEET, this);
        pageControl.setShowDivider(true);

        DBNNode node = getEditorInput().getNavigatorNode();

        Composite container = new Composite(pageControl, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 5;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        container.setLayout(gl);

        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (node == null) {
            return;
        }
        pageControl.createProgressPanel();

        createPropertyBrowser(container);
    }

    private void createPropertyBrowser(Composite container)
    {
        // Properties
        Composite propsPlaceholder = new Composite(container, SWT.NONE);
        GridData gd = new GridData(GridData.FILL_BOTH);
        //gd.horizontalSpan = 2;
        propsPlaceholder.setLayoutData(gd);
        GridLayout gl = new GridLayout(1, false);
        gl.horizontalSpacing = 0;
        gl.verticalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        propsPlaceholder.setLayout(gl);

        FolderInfo[] folders = collectFolders(this);
        boolean single = folders.length < 4;
        if (single) {
            for (FolderInfo fi : folders) {
                if (!fi.isEmbeddable()) {
                    single = false;
                }
            }
        }

        folderComposite = new FolderComposite(propsPlaceholder, SWT.LEFT | (single ? SWT.SINGLE : SWT.MULTI));
        folderComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Load properties
        folderComposite.setFolders(folders);

        // Collect section contributors
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        for (FolderInfo folder : folders) {
            IFolder page = folder.getContents();
            if (page instanceof IDatabaseEditorContributorUser) {
                IEditorActionBarContributor contributor = ((IDatabaseEditorContributorUser) page).getContributor(contributorManager);
                if (contributor != null) {
                    contributorManager.addContributor(contributor, this);
                    pageContributors.put(page, contributor);
                }
            }
            if (page instanceof ISaveablePart) {
                nestedSaveable.add((ISaveablePart) page);
            }
        }

        final String folderId = getEditorInput().getDefaultFolderId();
        if (folderId != null) {
            folderComposite.switchFolder(folderId);
        }

        folderComposite.addFolderListener(new IFolderListener() {
            @Override
            public void folderSelected(String folderId) {
                if (CommonUtils.equalObjects(curFolderId, folderId)) {
                    return;
                }
                synchronized (folderListeners) {
                    curFolderId = folderId;
                    for (IFolderListener listener : folderListeners) {
                        listener.folderSelected(folderId);
                    }
                }
            }

        });
    }

    @Override
    public void activatePart()
    {
        //getSite().setSelectionProvider();
    }

    @Override
    public void dispose()
    {
        // Remove contributors
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        for (IEditorActionBarContributor contributor : pageContributors.values()) {
            contributorManager.removeContributor(contributor, this);
        }
        pageContributors.clear();
        //PropertiesContributor.getInstance().removeLazyListener(this);

        super.dispose();
    }

    @Override
    public void setFocus()
    {
        // do not force focus in active editor. We can't do it properly because folderComposite detects
        // active folder by focus (which it doesn't have)
        //folderComposite.setFocus();
        IFolder selectedPage = folderComposite.getActiveFolder();
        if (selectedPage != null) {
            selectedPage.setFocus();
//            IEditorActionBarContributor contributor = pageContributors.get(selectedPage);
        }
    }

    @Override
    public void doSave(IProgressMonitor monitor)
    {
        for (ISaveablePart sp : nestedSaveable) {
            sp.doSave(monitor);
        }
    }

    @Override
    public void doSaveAs()
    {
        Object activeFolder = getActiveFolder();
        if (activeFolder instanceof ISaveablePart) {
            ((ISaveablePart) activeFolder).doSaveAs();
        }
    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        setInput(input);
    }

    @Override
    public boolean isDirty()
    {
        for (ISaveablePart sp : nestedSaveable) {
            if (sp.isDirty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }


    @Nullable
    @Override
    public ProgressPageControl getProgressControl()
    {
        return pageControl;
    }

    @Nullable
    @Override
    public IFolder getActiveFolder()
    {
        return folderComposite.getActiveFolder();
    }

    @Override
    public void switchFolder(String folderId)
    {
        folderComposite.switchFolder(folderId);
    }

    @Override
    public void addFolderListener(IFolderListener listener)
    {
        synchronized (folderListeners) {
            folderListeners.add(listener);
        }
    }

    @Override
    public void removeFolderListener(IFolderListener listener)
    {
        synchronized (folderListeners) {
            folderListeners.remove(listener);
        }
    }

    @Nullable
    private ISearchContextProvider getFolderSearch()
    {
        Object activeFolder = getActiveFolder();
        if (activeFolder instanceof ISearchContextProvider) {
            return (ISearchContextProvider)activeFolder;
        }
        return null;
    }

    @Override
    public boolean isSearchPossible()
    {
        return true;
    }

    @Override
    public boolean isSearchEnabled()
    {
        ISearchContextProvider provider = getFolderSearch();
        return provider != null && provider.isSearchEnabled();
    }

    @Override
    public boolean performSearch(SearchType searchType)
    {
        return getFolderSearch().performSearch(searchType);
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (folderComposite != null && folderComposite.getFolders() != null) {
            for (FolderInfo folder : folderComposite.getFolders()) {
                if (folder.getContents() instanceof IRefreshablePart) {
                    ((IRefreshablePart) folder.getContents()).refreshPart(source, force);
                }
            }
        }
    }

    @Override
    public Object getAdapter(Class adapter)
    {
        Object result = null;
        final Object activeFolder = getActiveFolder();
        if (activeFolder != null) {
            if (adapter.isAssignableFrom(activeFolder.getClass())) {
                result = activeFolder;
            } else if (activeFolder instanceof IAdaptable) {
                result = ((IAdaptable) activeFolder).getAdapter(adapter);
            }
        }
        return result == null ? super.getAdapter(adapter) : result;
    }

    public FolderInfo[] collectFolders(IWorkbenchPart part)
    {
        List<FolderInfo> tabList = new ArrayList<>();
        makeStandardPropertiesTabs(tabList);
        if (part instanceof IDatabaseEditor) {
            makeDatabaseEditorTabs((IDatabaseEditor)part, tabList);
        }
        return tabList.toArray(new FolderInfo[tabList.size()]);
    }

    private void makeStandardPropertiesTabs(List<FolderInfo> tabList)
    {
        tabList.add(new FolderInfo(
            //PropertiesContributor.CATEGORY_INFO,
            PropertiesContributor.TAB_STANDARD,
            CoreMessages.ui_properties_category_information,
            DBIcon.TREE_INFO,
            "General information",
            false,
            new FolderPageProperties(getEditorInput())));
    }

    private void makeDatabaseEditorTabs(final IDatabaseEditor part, final List<FolderInfo> tabList)
    {
        final DBNDatabaseNode node = part.getEditorInput().getNavigatorNode();
        final DBSObject object = node.getObject();

        // Collect tabs from navigator tree model
        DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor)
            {
                collectNavigatorTabs(monitor, part, node, tabList);
            }
        };
        try {
            if (node.needsInitialization()) {
                DBeaverUI.runInProgressService(tabsCollector);
            } else {
                tabsCollector.run(VoidProgressMonitor.INSTANCE);
            }
        } catch (InvocationTargetException e) {
            log.error(e.getTargetException());
        } catch (InterruptedException e) {
            // just go further
        }

        // Query for entity editors
        List<EntityEditorDescriptor> editors = EntityEditorsRegistry.getInstance().getEntityEditors(object, null);
        if (!CommonUtils.isEmpty(editors)) {
            for (EntityEditorDescriptor descriptor : editors) {
                if (descriptor.getType() == EntityEditorDescriptor.Type.folder) {
                    tabList.add(new FolderInfo(
                        descriptor.getId(),
                        descriptor.getName(),
                        descriptor.getIcon(),
                        descriptor.getDescription(),
                        descriptor.isEmbeddable(),
                        new FolderPageEditor(this, descriptor)));
                }
            }
        }
    }

    private static void collectNavigatorTabs(DBRProgressMonitor monitor, IDatabaseEditor part, DBNNode node, List<FolderInfo> tabList)
    {
        // Add all nested folders as tabs
        if (node instanceof DBNDataSource && !((DBNDataSource)node).getDataSourceContainer().isConnected()) {
            // Do not add children tabs
        } else if (node != null) {
            try {
                DBNNode[] children = node.getChildren(monitor);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder)child;
                            monitor.subTask(CoreMessages.ui_properties_task_add_folder + child.getNodeName() + "'"); //$NON-NLS-2$
                            tabList.add(
                                new FolderInfo(
                                    folder.getNodeName(),
                                    folder.getNodeName(),
                                    folder.getNodeIconDefault(),
                                    child.getNodeDescription(),
                                    false,//folder.getMeta().isInline(),
                                    new FolderPageNode(part, folder, null)
                                ));
                        }
                    }
                }
            } catch (DBException e) {
                log.error("Error initializing property tabs", e); //$NON-NLS-1$
            }
            // Add itself as tab (if it has child items)
            if (node instanceof DBNDatabaseNode) {
                DBNDatabaseNode databaseNode = (DBNDatabaseNode)node;
                List<DBXTreeNode> subNodes = databaseNode.getMeta().getChildren(databaseNode);
                if (subNodes != null) {
                    for (DBXTreeNode child : subNodes) {
                        if (child instanceof DBXTreeItem) {
                            try {
                                if (!((DBXTreeItem)child).isOptional() || databaseNode.hasChildren(monitor, child)) {
                                    monitor.subTask(CoreMessages.ui_properties_task_add_node + node.getNodeName() + "'"); //$NON-NLS-2$
                                    String nodeName = child.getChildrenType(databaseNode.getObject().getDataSource());
                                    tabList.add(
                                        new FolderInfo(
                                            nodeName,
                                            nodeName,
                                            node.getNodeIconDefault(),
                                            node.getNodeDescription(),
                                            false,
                                            new FolderPageNode(part, node, child)));
                                }
                            } catch (DBException e) {
                                log.debug("Can't add child items tab", e); //$NON-NLS-1$
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public DBNNode getRootNode() {
        IFolder activeFolder = folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getRootNode();
        }
        return null;
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer() {
        IFolder activeFolder = folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getNavigatorViewer();
        }
        return null;
    }
}