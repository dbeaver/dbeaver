/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.*;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorContributorUser;
import org.jkiss.dbeaver.ui.editors.entity.GlobalContributorManager;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
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
    implements IRefreshablePart, IProgressControlProvider, ITabbedFolderContainer, ISearchContextProvider, INavigatorModelView
{
    private static final Log log = Log.getLog(ObjectPropertiesEditor.class);

    private TabbedFolderComposite folderComposite;
    private ObjectEditorPageControl pageControl;
    private final List<ITabbedFolderListener> folderListeners = new ArrayList<>();
    private String curFolderId;

    private final List<ISaveablePart> nestedSaveable = new ArrayList<>();
    private final Map<ITabbedFolder, IEditorActionBarContributor> pageContributors = new HashMap<>();
    private SashForm sashForm;
    private boolean activated = false;
    private Composite propsPlaceholder;
    private TabbedFolderPageProperties propertiesPanel;

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

        Composite container = new Composite(pageControl, SWT.NONE);
        GridLayout gl = new GridLayout(1, false);
        gl.verticalSpacing = 5;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        container.setLayout(gl);

        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        pageControl.createProgressPanel();

        createPropertyBrowser(container);
    }

    private void createPropertyBrowser(Composite container)
    {
        TabbedFolderInfo[] folders = collectFolders(this);
        if (folders.length == 0) {
            createPropertiesPanel(container);
        } else {
            sashForm = UIUtils.createPartDivider(getSite().getPart(), container, SWT.VERTICAL);
            sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));

            createPropertiesPanel(sashForm);
            createFoldersPanel(sashForm, folders);
        }
    }

    private void createPropertiesPanel(Composite container) {
        // Main panel
        propsPlaceholder = UIUtils.createPlaceholder(container, 2, 0);
        propsPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));
    }

    private void createFoldersPanel(Composite parent, TabbedFolderInfo[] folders) {
        // Properties
        Composite foldersPlaceholder = UIUtils.createPlaceholder(parent, 1, 0);
        foldersPlaceholder.setLayoutData(new GridData(GridData.FILL_BOTH));

        boolean single = folders.length < 4;
        if (single) {
            for (TabbedFolderInfo fi : folders) {
                if (!fi.isEmbeddable()) {
                    single = false;
                }
            }
        }

        folderComposite = new TabbedFolderComposite(foldersPlaceholder, SWT.LEFT | (single ? SWT.SINGLE : SWT.MULTI));
        folderComposite.setLayoutData(new GridData(GridData.FILL_BOTH));

        // Load properties
        {
            String objectId = "PropertiesEditor." + getDatabaseObject().getClass().getName();
            folderComposite.setFolders(objectId, folders);
        }

        // Collect section contributors
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        for (TabbedFolderInfo folder : folders) {
            ITabbedFolder page = folder.getContents();
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
        folderComposite.switchFolder(folderId);

        folderComposite.addFolderListener(new ITabbedFolderListener() {
            @Override
            public void folderSelected(String folderId) {
                if (CommonUtils.equalObjects(curFolderId, folderId)) {
                    return;
                }
                synchronized (folderListeners) {
                    curFolderId = folderId;
                    for (ITabbedFolderListener listener : folderListeners) {
                        listener.folderSelected(folderId);
                    }
                }
            }

        });
    }

    private void updateSashWidths() {
        if (sashForm.isDisposed()) {
            return;
        }

        Control[] children = sashForm.getChildren();
        if (children.length < 2) {
            // Unexpected
            return;
        }
        Point foldersSize = children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT);
        //Point foldersSize = children[1].computeSize(SWT.DEFAULT, SWT.DEFAULT);
        Point sashSize = sashForm.getSize();
        if (sashSize.y > 0 && foldersSize.y > sashSize.y / 2) {
            int[] weights = new int[] {
                (sashSize.y - foldersSize.y),
                foldersSize.y
            };
            sashForm.setWeights(weights);
        } else {
            sashForm.setWeights(new int[] { 400, 600 });
        }
        sashForm.layout();
    }

    @Override
    public void activatePart()
    {
        if (activated) {
            return;
        }
        activated = true;
        propertiesPanel = new TabbedFolderPageProperties(this, getEditorInput());

        propertiesPanel.createControl(propsPlaceholder);

        pageControl.layout(true);
        propsPlaceholder.layout(true);

        if (sashForm != null) {
            Runnable sashUpdater = new Runnable() {
                @Override
                public void run() {
                    updateSashWidths();
                }
            };
            if (sashForm.getSize().y > 0) {
                sashUpdater.run();
            } else {
                DBeaverUI.asyncExec(sashUpdater);
            }
        }
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
        if (folderComposite != null) {
            ITabbedFolder selectedPage = folderComposite.getActiveFolder();
            if (selectedPage != null) {
                selectedPage.setFocus();
                //            IEditorActionBarContributor contributor = pageContributors.get(selectedPage);
            }
        } else if (pageControl != null) {
            pageControl.setFocus();
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
    public ITabbedFolder getActiveFolder()
    {
        return folderComposite == null ? null : folderComposite.getActiveFolder();
    }

    @Override
    public void switchFolder(String folderId)
    {
        if (folderComposite != null) {
            folderComposite.switchFolder(folderId);
        }
    }

    @Override
    public void addFolderListener(ITabbedFolderListener listener)
    {
        synchronized (folderListeners) {
            folderListeners.add(listener);
        }
    }

    @Override
    public void removeFolderListener(ITabbedFolderListener listener)
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
        ISearchContextProvider folderSearch = getFolderSearch();
        if (folderSearch != null) {
            return folderSearch.performSearch(searchType);
        } else {
            return false;
        }
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (propertiesPanel != null) {
            propertiesPanel.refreshPart(source, force);
        }
        if (folderComposite != null && folderComposite.getFolders() != null) {
            for (TabbedFolderInfo folder : folderComposite.getFolders()) {
                if (folder.getContents() instanceof IRefreshablePart) {
                    ((IRefreshablePart) folder.getContents()).refreshPart(source, force);
                }
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter)
    {
        Object result = null;
        final Object activeFolder = getActiveFolder();
        if (activeFolder != null) {
            if (activeFolder instanceof IAdaptable) {
                result = ((IAdaptable) activeFolder).getAdapter(adapter);
            } else if (adapter.isAssignableFrom(activeFolder.getClass())) {
                result = activeFolder;
            }
        }
        if (result != null) {
            return adapter.cast(result);
        }
        return null;//super.getAdapter(adapter);
    }

    public TabbedFolderInfo[] collectFolders(IWorkbenchPart part)
    {
        List<TabbedFolderInfo> tabList = new ArrayList<>();
        //makeStandardPropertiesTabs(tabList);
        if (part instanceof IDatabaseEditor) {
            makeDatabaseEditorTabs((IDatabaseEditor)part, tabList);
        }
        return tabList.toArray(new TabbedFolderInfo[tabList.size()]);
    }

    private void makeStandardPropertiesTabs(List<TabbedFolderInfo> tabList)
    {
        tabList.add(new TabbedFolderInfo(
            //PropertiesContributor.CATEGORY_INFO,
            PropertiesContributor.TAB_STANDARD,
            CoreMessages.ui_properties_category_information,
            DBIcon.TREE_INFO,
            "General information",
            false,
            new TabbedFolderPageProperties(this, getEditorInput())));
    }

    private void makeDatabaseEditorTabs(final IDatabaseEditor part, final List<TabbedFolderInfo> tabList)
    {
        final DBNDatabaseNode node = part.getEditorInput().getNavigatorNode();
        if (node == null) {
            return;
        }
        final DBSObject object = node.getObject();

        if (!node.getMeta().isStandaloneNode()) {
            // Collect tabs from navigator tree model
            DBRRunnableWithProgress tabsCollector = new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) {
                    collectNavigatorTabs(monitor, part, node, tabList);
                }
            };
            try {
                if (node.needsInitialization()) {
                    DBeaverUI.runInProgressService(tabsCollector);
                } else {
                    tabsCollector.run(new VoidProgressMonitor());
                }
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // just go further
            }
        }

        // Query for entity editors
        List<EntityEditorDescriptor> editors = EntityEditorsRegistry.getInstance().getEntityEditors(object, null);
        if (!CommonUtils.isEmpty(editors)) {
            for (EntityEditorDescriptor descriptor : editors) {
                if (descriptor.getType() == EntityEditorDescriptor.Type.folder) {
                    tabList.add(new TabbedFolderInfo(
                        descriptor.getId(),
                        descriptor.getName(),
                        descriptor.getIcon(),
                        descriptor.getDescription(),
                        descriptor.isEmbeddable(),
                        new TabbedFolderPageEditor(this, descriptor)));
                }
            }
        }
    }

    private static void collectNavigatorTabs(DBRProgressMonitor monitor, IDatabaseEditor part, DBNNode node, List<TabbedFolderInfo> tabList)
    {
        // Add all nested folders as tabs
        if (node instanceof DBNDataSource && !((DBNDataSource)node).getDataSourceContainer().isConnected()) {
            // Do not add children tabs
        } else if (node != null) {
            try {
                DBNNode[] children = NavigatorUtils.getNodeChildrenFiltered(monitor, node, false);
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder)child;
                            monitor.subTask(CoreMessages.ui_properties_task_add_folder + child.getNodeName() + "'"); //$NON-NLS-2$
                            tabList.add(
                                new TabbedFolderInfo(
                                    folder.getNodeName(),
                                    folder.getNodeName(),
                                    folder.getNodeIconDefault(),
                                    child.getNodeDescription(),
                                    false,//folder.getMeta().isInline(),
                                    new TabbedFolderPageNode(part, folder, null)
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
                                        new TabbedFolderInfo(
                                            nodeName,
                                            nodeName,
                                            node.getNodeIconDefault(),
                                            node.getNodeDescription(),
                                            false,
                                            new TabbedFolderPageNode(part, node, child)));
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
        ITabbedFolder activeFolder = folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getRootNode();
        }
        return null;
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer() {
        ITabbedFolder activeFolder = folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getNavigatorViewer();
        }
        return null;
    }
}