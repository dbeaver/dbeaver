/*
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.*;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabSelectionListener;
import org.eclipse.ui.views.properties.tabbed.TabContents;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.IDatabaseEditorContributorUser;
import org.jkiss.dbeaver.ext.ui.IProgressControlProvider;
import org.jkiss.dbeaver.ext.ui.*;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.ProxyPageSite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.editors.entity.GlobalContributorManager;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ObjectPropertiesEditor
 */
public class ObjectPropertiesEditor extends AbstractDatabaseObjectEditor<DBSObject>
    implements IRefreshablePart, IProgressControlProvider, IFolderedPart, ISearchContextProvider, IRefreshableContainer
{
    //static final Log log = Log.getLog(ObjectPropertiesEditor.class);

    private PropertyPageTabbed properties;
    private ObjectEditorPageControl pageControl;
    private final List<IFolderListener> folderListeners = new ArrayList<IFolderListener>();
    private String curFolderId;

    private final List<IRefreshablePart> refreshClients = new ArrayList<IRefreshablePart>();
    private final List<ISaveablePart> nestedSaveable = new ArrayList<ISaveablePart>();
    private final Map<ISection, IEditorActionBarContributor> sectionContributors = new HashMap<ISection, IEditorActionBarContributor>();
    //private Text nameText;
    //private Text descriptionText;

    public ObjectPropertiesEditor()
    {
    }

    @Override
    public void createPartControl(Composite parent)
    {
        // Add lazy props listener
        //PropertiesContributor.getInstance().addLazyListener(this);

        pageControl = new ObjectEditorPageControl(parent, SWT.NONE, this);

        DBNNode node = getEditorInput().getTreeNode();

        Composite container = new Composite(pageControl, SWT.NONE);
        GridLayout gl = new GridLayout(2, false);
        gl.verticalSpacing = 5;
        gl.horizontalSpacing = 0;
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        container.setLayout(gl);

        container.setLayoutData(new GridData(GridData.FILL_BOTH));

        if (node == null) {
            return;
        }
        createPathPanel(node, container);
        //createNamePanel(node, container);

        pageControl.createProgressPanel();

        createPropertyBrowser(container);
    }

    private void createPathPanel(DBNNode node, Composite container)
    {
        // Path
        Composite infoGroup = new Composite(container, SWT.BORDER);//createControlGroup(container, "Path", 3, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        infoGroup.setLayout(new RowLayout());

        List<DBNDatabaseNode> nodeList = new ArrayList<DBNDatabaseNode>();
        for (DBNNode n = node; n != null; n = n.getParentNode()) {
            if (n instanceof DBNDatabaseNode) {
                nodeList.add(0, (DBNDatabaseNode)n);
            }
        }
        for (final DBNDatabaseNode databaseNode : nodeList) {
            createPathRow(
                infoGroup,
                databaseNode.getNodeIconDefault(),
                databaseNode.getNodeType(),
                databaseNode.getNodeName(),
                databaseNode == node ? null : new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e)
                    {
                        NavigatorHandlerObjectOpen.openEntityEditor(databaseNode, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                    }
                });
        }
    }

    private void createPropertyBrowser(Composite container)
    {
        // Properties
        Composite propsPlaceholder = new Composite(container, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        propsPlaceholder.setLayoutData(gd);
        propsPlaceholder.setLayout(new FormLayout());

        //final PropertyCollector propertyCollector = new PropertyCollector(itemObject);
        //List<ObjectPropertyDescriptor> annoProps = ObjectPropertyDescriptor.extractAnnotations(itemObject);

        properties = new PropertyPageTabbed();
        properties.init(new ProxyPageSite(getSite()));
        properties.createControl(propsPlaceholder);

        // Load properties
        loadObjectProperties();

        // Collect section contributors
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        for (ITabDescriptor tab : properties.getActiveTabs()) {
            final ISection[] tabSections = properties.getTabSections(tab);
            if (!ArrayUtils.isEmpty(tabSections)) {
                for (ISection section : tabSections) {
                    if (section instanceof IDatabaseEditorContributorUser) {
                        IEditorActionBarContributor contributor = ((IDatabaseEditorContributorUser) section).getContributor(contributorManager);
                        if (contributor != null) {
                            contributorManager.addContributor(contributor, this);
                            sectionContributors.put(section, contributor);
                        }
                    }
                    if (section instanceof ISaveablePart) {
                        nestedSaveable.add((ISaveablePart) section);
                    }
                }
            }
        }

        final String folderId = getEditorInput().getDefaultFolderId();
        if (folderId != null) {
            properties.setSelectedTab(folderId);
        }

        properties.addTabSelectionListener(new ITabSelectionListener() {
            @Override
            public void tabSelected(ITabDescriptor tabDescriptor)
            {
                if (CommonUtils.equalObjects(curFolderId, tabDescriptor.getId())) {
                    return;
                }
                synchronized (folderListeners) {
                    curFolderId = tabDescriptor.getId();
                    for (IFolderListener listener : folderListeners) {
                        listener.folderSelected(tabDescriptor.getId());
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

    private void loadObjectProperties()
    {
        properties.selectionChanged(
            this,
            new StructuredSelection(
                getEditorInput().getPropertySource()));
    }

/*
    private void createNamePanel(DBNNode node, Composite container)
    {
        // General options
        Group infoGroup = UIUtils.createControlGroup(container, "General", 2, GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        UIUtils.createControlLabel(infoGroup, "Name");
        nameText = new Text(infoGroup, SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 200;
        nameText.setLayoutData(gd);
        nameText.setText(node.getNodeName());
        nameText.setEditable(isNameEditable());

        Label descriptionLabel = UIUtils.createControlLabel(infoGroup, "Description");
        descriptionLabel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
        descriptionText = new Text(infoGroup, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
        gd.widthHint = 200;
        gd.heightHint = descriptionLabel.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).y * 3;
        descriptionText.setLayoutData(gd);
        if (!CommonUtils.isEmpty(node.getNodeDescription())) {
            descriptionText.setText(node.getNodeDescription());
        }
        descriptionText.setEditable(isDescriptionEditable());
    }
*/

    @Override
    public void dispose()
    {
        // Remove contributors
        GlobalContributorManager contributorManager = GlobalContributorManager.getInstance();
        for (IEditorActionBarContributor contributor : sectionContributors.values()) {
            contributorManager.removeContributor(contributor, this);
        }
        sectionContributors.clear();
        //PropertiesContributor.getInstance().removeLazyListener(this);

        if (properties != null) {
            properties.dispose();
            properties = null;
        }

        super.dispose();
    }

    @Override
    public void setFocus()
    {
        properties.setFocus();
        ITabDescriptor selectedTab = properties.getSelectedTab();
        if (selectedTab != null) {
            final ISection[] tabSections = properties.getTabSections(selectedTab);
            if (!ArrayUtils.isEmpty(tabSections)) {
                for (ISection section : tabSections) {
                    IEditorActionBarContributor contributor = sectionContributors.get(section);
                    if (contributor != null) {
                        section.aboutToBeShown();
                    }
                }
            }
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

    private void createPathRow(Composite infoGroup, Image image, String label, String value, @Nullable SelectionListener selectionListener)
    {
        UIUtils.createImageLabel(infoGroup, image);
        //UIUtils.createControlLabel(infoGroup, label);

        Link objectLink = new Link(infoGroup, SWT.NONE);
        //objectLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (selectionListener == null) {
            objectLink.setText(value);
            objectLink.setToolTipText(label);
        } else {
            objectLink.setText("<A>" + value + "</A>   ");
            objectLink.addSelectionListener(selectionListener);
            objectLink.setToolTipText("Open " + label + " Editor");
        }
    }

/*
    public void handlePropertyLoad(final Object object, final Object propertyId, final Object propertyValue, final boolean completed)
    {
        if (completed && object == getTreeNode()) {
            if ("description".equals(propertyId)) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run()
                    {
                        descriptionText.setText(CommonUtils.toString(propertyValue));
                    }
                });
            }
        }
    }
*/

    @Nullable
    @Override
    public ProgressPageControl getProgressControl()
    {
        return pageControl;
    }

    @Nullable
    @Override
    public Object getActiveFolder()
    {
        TabContents currentTab = properties.getCurrentTab();
        if (currentTab != null) {
            ISection[] sections = currentTab.getSections();
            if (ArrayUtils.isEmpty(sections)) {
                return null;
            } else if (sections.length == 1) {
                return sections[0];
            } else {
                return sections;
            }
        }
        return null;
    }

    @Override
    public void switchFolder(String folderId)
    {
        properties.setSelectedTab(folderId);
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
    public void addRefreshClient(IRefreshablePart part)
    {
        synchronized (refreshClients) {
            refreshClients.add(part);
        }
    }

    @Override
    public void removeRefreshClient(IRefreshablePart part)
    {
        synchronized (refreshClients) {
            refreshClients.add(part);
        }
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        synchronized (refreshClients) {
            for (IRefreshablePart part : refreshClients) {
                part.refreshPart(source, force);
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

}