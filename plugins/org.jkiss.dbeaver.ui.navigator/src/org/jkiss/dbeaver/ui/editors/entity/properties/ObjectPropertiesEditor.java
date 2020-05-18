/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.PartSite;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeFolder;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.controls.folders.*;
import org.jkiss.dbeaver.ui.css.CSSUtils;
import org.jkiss.dbeaver.ui.css.DBStyles;
import org.jkiss.dbeaver.ui.editors.*;
import org.jkiss.dbeaver.ui.editors.entity.*;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * ObjectPropertiesEditor
 */
public class ObjectPropertiesEditor extends AbstractDatabaseObjectEditor<DBSObject> implements IEntityStructureEditor,
        IRefreshablePart,
        IProgressControlProvider,
        ITabbedFolderContainer,
        ISearchContextProvider,
        INavigatorModelView,
        IEntityEditorContext,
        IDatabasePostSaveProcessor
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
    @Nullable
    private TabbedFolderPageForm propertiesPanel;

    public ObjectPropertiesEditor()
    {
    }

    @Override
    public void createPartControl(Composite parent)
    {
        // Add lazy props listener
        //PropertiesContributor.getInstance().addLazyListener(this);
        pageControl = new ObjectEditorPageControl(parent, SWT.SHEET, this) {
            @Override
            public void fillCustomActions(IContributionManager contributionManager) {
                createPropertyRefreshAction(contributionManager);

                super.fillCustomActions(contributionManager);
                if (propertiesPanel != null && folderComposite == null) {
                    // We have object editor and no folders - contribute default actions
                    DatabaseEditorUtils.contributeStandardEditorActions(getSite(), contributionManager);
                }
            }
        };
        CSSUtils.setCSSClass(pageControl, DBStyles.COLORED_BY_CONNECTION_TYPE);
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

        curFolderId = getEditorInput().getDefaultFolderId();

        // Create actual editor in async mode. We need to know editor size to make proper layout and avoid blinking
        UIUtils.asyncExec(() -> createPropertyBrowser(container));
    }

    private void createPropertyBrowser(Composite container)
    {
        pageControl.setRedraw(false);
        try {
            TabbedFolderInfo[] folders = collectFolders(this);
            if (folders.length == 0) {
                createPropertiesPanel(container);
            } else {
                Composite foldersParent = container;
                if (hasPropertiesEditor() && DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.ENTITY_EDITOR_DETACH_INFO)) {
                    sashForm = UIUtils.createPartDivider(getSite().getPart(), container, SWT.VERTICAL);
                    sashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
                    foldersParent = sashForm;

                    createPropertiesPanel(sashForm);
                }
                createFoldersPanel(foldersParent, folders);
            }

            // Create props
            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.ENTITY_EDITOR_DETACH_INFO)) {
                if (hasPropertiesEditor()) {
                    propertiesPanel = new TabbedFolderPageForm(this, pageControl, getEditorInput());

                    propertiesPanel.createControl(propsPlaceholder);
                }
            }

            if (sashForm != null) {
                //Runnable sashUpdater = this::updateSashWidths;
                //sashUpdater.run();
                //UIUtils.asyncExec(sashUpdater);
                updateSashWidths();
            }
            pageControl.layout(true, true);
        } finally {
            pageControl.setRedraw(true);
        }
    }

    private void createPropertiesPanel(Composite container) {
        // Main panel
        propsPlaceholder = new Composite(container, SWT.NONE);
        propsPlaceholder.setLayout(new FillLayout());
    }

    private Composite createFoldersPanel(Composite parent, TabbedFolderInfo[] folders) {
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

        folderComposite.switchFolder(curFolderId);

        folderComposite.addFolderListener(folderId1 -> {
            if (CommonUtils.equalObjects(curFolderId, folderId1)) {
                return;
            }

            IActionBars actionBars = getEditorSite().getActionBars();
            MultiPageEditorPart mainEditor = ((MultiPageEditorSite) getSite()).getMultiPageEditor();
            IWorkbenchPartSite mainEditorSite = mainEditor.getSite();
            if (mainEditorSite instanceof PartSite) {
                ((PartSite) mainEditorSite).deactivateActionBars(true);
            }

            ITabbedFolder activeFolder = folderComposite.getActiveFolder();
            if (activeFolder instanceof TabbedFolderPageEditor) {
                IEditorActionBarContributor activeFolderContributor = pageContributors.get(activeFolder);
                if (activeFolderContributor != null) {
                    // FIXME: do not add extra contributions as they will be there forever (never cleaned up)
//                    if (activeFolderContributor instanceof EditorActionBarContributor) {
//                        ((EditorActionBarContributor) activeFolderContributor).contributeToStatusLine(
//                            actionBars.getStatusLineManager());
//                    }
                    activeFolderContributor.setActiveEditor(((TabbedFolderPageEditor) activeFolder).getEditor());
                }
            }
            actionBars.updateActionBars();

            synchronized (folderListeners) {
                curFolderId = folderId1;
                for (ITabbedFolderListener listener : folderListeners) {
                    listener.folderSelected(folderId1);
                }
            }
        });
        return foldersPlaceholder;
    }

    public static Point getParentSize(Control control) {
        for (Composite composite = control.getParent(); composite != null; composite = composite.getParent()) {
            if (composite instanceof CTabFolder) {
                Point size = composite.getSize();
                if (size.x > 0 && size.y > 0) {
                    return size;
                }
            }
        }
        return new Point(0, 0);
    }

    private void updateSashWidths() {
        if (sashForm.isDisposed()) {
            return;
        }

//        if (propsPlaceholder != null) {
            Point propsSize = propsPlaceholder.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
            propsSize.y += 10;
            Point sashSize = sashForm.getParent().getSize();
            if (sashSize.x <= 0 || sashSize.y <= 0) {
                // This may happen if EntityEditor created with some other active editor (i.e. props editor not visible)
                sashSize = getParentSize(sashForm);
                //sashSize.y += 20;
            }
            if (sashSize.x > 0 && sashSize.y > 0) {
                float ratio = (float) propsSize.y / (float) sashSize.y;
                int propsRatio = Math.min(1000, (int) (1000 * ratio));
                int[] newWeights = {propsRatio, 1000 - propsRatio};
                if (!Arrays.equals(newWeights, sashForm.getWeights())) {
                    sashForm.setWeights(newWeights);
                    //sashForm.layout();
                }
            }

/*
        } else {
            String sashStateStr = DBWorkbench.getPlatform().getPreferenceStore().getString(NavigatorPreferences.ENTITY_EDITOR_INFO_SASH_STATE);
            int sashPanelHeight = !CommonUtils.isEmpty(sashStateStr) ? Integer.parseInt(sashStateStr) : 400;
            if (sashPanelHeight < 0) sashPanelHeight = 0;
            if (sashPanelHeight > 1000) sashPanelHeight = 1000;

            sashForm.setWeights(new int[] { sashPanelHeight, 1000 - sashPanelHeight });
            //sashForm.layout();

            sashForm.getChildren()[0].addListener(SWT.Resize, event -> {
                if (sashForm != null) {
                    int[] weights = sashForm.getWeights();
                    if (weights != null && weights.length > 0) {
                        int topWeight = weights[0];
                        if (topWeight == 0) topWeight = 1;
                        DBWorkbench.getPlatform().getPreferenceStore().setValue(NavigatorPreferences.ENTITY_EDITOR_INFO_SASH_STATE, topWeight);
                    }
                }
            });
        }
*/
    }

    @Override
    public void activatePart()
    {
        if (activated) {
            return;
        }
        activated = true;
    }

    private boolean hasPropertiesEditor() {
        return !(getEditorInput().getDatabaseObject() instanceof DBNDatabaseFolder);
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
            if (sp.isDirty()) {
                sp.doSave(monitor);
            }
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
        return getActiveFolder(true);
    }

    private ITabbedFolder getActiveFolder(boolean activate)
    {
        return folderComposite == null ? null : folderComposite.getActiveFolder(activate);
    }

    @Override
    public boolean switchFolder(String folderId)
    {
        this.curFolderId = folderId;
        if (folderComposite != null) {
            return folderComposite.switchFolder(curFolderId);
        }
        return false;
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
        final Object activeFolder = getActiveFolder(false);
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
        makeStandardPropertiesTabs(tabList);
        if (part instanceof IDatabaseEditor) {
            makeDatabaseEditorTabs((IDatabaseEditor)part, tabList);
        }
        return tabList.toArray(new TabbedFolderInfo[0]);
    }

    private void makeStandardPropertiesTabs(List<TabbedFolderInfo> tabList)
    {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.ENTITY_EDITOR_DETACH_INFO)) {
            tabList.add(new TabbedFolderInfo(
                //PropertiesContributor.CATEGORY_INFO,
                PropertiesContributor.TAB_STANDARD,
                UINavigatorMessages.ui_properties_category_information,
                DBIcon.TREE_INFO,
                UINavigatorMessages.ui_properties_category_information_tip,
                false,
                new TabbedFolderPageForm(this, pageControl, getEditorInput())));
        }
    }

    private void makeDatabaseEditorTabs(final IDatabaseEditor part, final List<TabbedFolderInfo> tabList)
    {
        final DBNDatabaseNode node = part.getEditorInput() instanceof IDatabaseEditorInput ? ((IDatabaseEditorInput) part.getEditorInput()).getNavigatorNode() : null;
        if (node == null) {
            return;
        }
        final DBSObject object = node.getObject();

        if (!node.getMeta().isStandaloneNode()) {
            // Collect tabs from navigator tree model
            DBRRunnableWithProgress tabsCollector = monitor ->
                collectNavigatorTabs(monitor, part, node, tabList);
            try {
                if (node.needsInitialization()) {
                    UIUtils.runInProgressService(tabsCollector);
                } else {
                    tabsCollector.run(new VoidProgressMonitor());
                }
            } catch (InvocationTargetException e) {
                log.error(e.getTargetException());
            } catch (InterruptedException e) {
                // just go further
            }
        }

        // Extra properties tab (show if we have extra properties only)
        {
            TabbedFolderPageProperties pageProperties = new TabbedFolderPageProperties(this, getEditorInput());
            List<String> extraCategories = pageProperties.getExtraCategories();
            if (!extraCategories.isEmpty()) {
                tabList.add(new TabbedFolderInfo(
                    PropertiesContributor.TAB_PROPERTIES,
                    extraCategories.get(0) + (extraCategories.size() == 1 ? "" :" / ... "),
                    DBIcon.TREE_INFO,
                    String.join(", ", extraCategories),
                    false,
                    pageProperties));
            }
        }

        // Query for entity editors
        List<EntityEditorDescriptor> editors = EntityEditorsRegistry.getInstance().getEntityEditors(object, this, null);
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
                DBNNode[] children = DBNUtils.getNodeChildrenFiltered(monitor, node, false);
                if (node instanceof DBNDatabaseNode && ((DBNDatabaseNode) node).getDataSourceContainer().getNavigatorSettings().isHideFolders()) {
                    if (children != null) {
                        // Folders are hidden in navigator. But we must show them here for all present child items
                        Map<DBXTreeFolder, List<DBNNode>> childMap = new LinkedHashMap<>();
                        for (DBNNode child : children) {
                            if (child instanceof DBNDatabaseNode) {
                                DBXTreeNode meta = ((DBNDatabaseNode) child).getMeta();
                                if (meta.getParent() instanceof DBXTreeFolder) {
                                    List<DBNNode> itemList = childMap.computeIfAbsent((DBXTreeFolder) meta.getParent(), dbxTreeFolder -> new ArrayList<>());
                                    itemList.add(child);
                                }
                            }
                        }
                        for (Map.Entry<DBXTreeFolder, List<DBNNode>> fe : childMap.entrySet()) {
                            DBXTreeFolder folder = fe.getKey();
                            String nodeName = folder.getChildrenTypeLabel(((DBNDatabaseNode) node).getObject().getDataSource(), null);
                            tabList.add(
                                new TabbedFolderInfo(
                                    nodeName,
                                    nodeName,
                                    folder.getDefaultIcon(),
                                    folder.getDescription(),
                                    false,
                                    new TabbedFolderPageNode(part, node, folder)));
                        }
                    }
                }
                if (children != null) {
                    for (DBNNode child : children) {
                        if (child instanceof DBNDatabaseFolder) {
                            DBNDatabaseFolder folder = (DBNDatabaseFolder)child;
                            monitor.subTask(UINavigatorMessages.ui_properties_task_add_folder + child.getNodeName() + "'"); //$NON-NLS-2$
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
                                    monitor.subTask(UINavigatorMessages.ui_properties_task_add_node + node.getNodeName() + "'"); //$NON-NLS-2$
                                    String nodeName = child.getChildrenTypeLabel(databaseNode.getObject().getDataSource(), null);
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
        ITabbedFolder activeFolder = folderComposite == null ? null : folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getRootNode();
        }
        return null;
    }

    @Nullable
    @Override
    public Viewer getNavigatorViewer() {
        ITabbedFolder activeFolder = folderComposite == null ? null : folderComposite.getActiveFolder();
        if (activeFolder instanceof INavigatorModelView) {
            return ((INavigatorModelView) activeFolder).getNavigatorViewer();
        }
        return null;
    }

    // This is used by extensions to determine whether this entity is another entity container (e.g. for ERD)
    @Override
    public boolean isEntityContainer(DBSObjectContainer object) {
        return false;
    }

    @Override
    public boolean isRelationalObject(DBSObject object) {
        return true;
    }

    @Override
    public void runPostSaveCommands(Map<String, Object> context) {
        for (ISaveablePart sp : nestedSaveable) {
            if (sp instanceof TabbedFolderPageEditor) {
                IEditorPart editor = ((TabbedFolderPageEditor) sp).getEditor();
                if (editor instanceof IDatabasePostSaveProcessor) {
                    ((IDatabasePostSaveProcessor) editor).runPostSaveCommands(context);
                }
            }
        }
    }

    private void createPropertyRefreshAction(IContributionManager contributionManager) {
        // Contribute "Read expensive props" - but only if object has expensive props
        DBSObject databaseObject = getDatabaseObject();
        if (!databaseObject.getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.READ_EXPENSIVE_PROPERTIES)) {
            PropertyCollector collector = new PropertyCollector(databaseObject, false);
            collector.setEnableFilters(false);
            collector.collectProperties();

            boolean hasExpensive = false;
            for (DBPPropertyDescriptor prop : collector.getPropertyDescriptors2()) {
                if (prop instanceof ObjectPropertyDescriptor && ((ObjectPropertyDescriptor) prop).isExpensive()) {
                    hasExpensive = true;
                    break;
                }
            }

            if (hasExpensive) {
                contributionManager.add(new ReadExpensivePropsAction(databaseObject));
            }
        }
    }

    private class ReadExpensivePropsAction extends Action {
        private final DBSObject databaseObject;
        ReadExpensivePropsAction(DBSObject databaseObject) {
            super("Read row count and other expensive properties", AS_CHECK_BOX);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.OBJ_REFRESH));
            this.databaseObject = databaseObject;
        }

        @Override
        public boolean isChecked() {
            return DataSourcePropertyFilter.isExpensivePropertiesReadEnabledFor(this.databaseObject);
        }

        @Override
        public void run() {
            DataSourcePropertyFilter.readExpensivePropertiesFor(this.databaseObject, !isChecked());
            DBUtils.fireObjectUpdate(this.databaseObject, true);

            MultiPageEditorPart mainEditor = ((MultiPageEditorSite) getSite()).getMultiPageEditor();
            if (mainEditor instanceof IRefreshablePart) {
                ((IRefreshablePart) mainEditor).refreshPart(this, true);
            }
            //getRootNode().refreshNode()
            //DBWorkbench.getPlatform().getNavigatorModel().fireNodeUpdate(source, this, DBNEvent.NodeChange.REFRESH);
        }
    }
}