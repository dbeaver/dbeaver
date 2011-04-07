/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.viewers.IFilter;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.tabbed.ISection;
import org.eclipse.ui.views.properties.tabbed.ITabDescriptor;
import org.eclipse.ui.views.properties.tabbed.ITabSelectionListener;
import org.eclipse.ui.views.properties.tabbed.TabContents;
import org.jkiss.dbeaver.ext.IProgressControlProvider;
import org.jkiss.dbeaver.ext.ui.IFolderListener;
import org.jkiss.dbeaver.ext.ui.IFolderedPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.ext.ui.ISearchContextProvider;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.controls.ProgressPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.views.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.views.properties.ProxyPageSite;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultObjectEditor
 */
public class DefaultObjectEditor extends AbstractDatabaseObjectEditor implements IRefreshablePart, IProgressControlProvider, IFolderedPart, ISearchContextProvider//, ILazyPropertyLoadListener
{
    //static final Log log = LogFactory.getLog(DefaultObjectEditor.class);

    private PropertyPageTabbed properties;
    private ObjectEditorPageControl pageControl;
    private final List<IFolderListener> folderListeners = new ArrayList<IFolderListener>();
    private String curFolderId;
    //private Text nameText;
    //private Text descriptionText;

    public DefaultObjectEditor()
    {
    }

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
        createPropertyBrowser(container);

        pageControl.createProgressPanel();
    }

    private void createPathPanel(DBNNode node, Composite container)
    {
        // Path
        Composite infoGroup = new Composite(container, SWT.BORDER);//createControlGroup(container, "Path", 3, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
        infoGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        infoGroup.setLayout(new RowLayout());

/*
            if (node instanceof DBNDatabaseNode) {
                DBNDatabaseNode dbNode = (DBNDatabaseNode)node;
                if (dbNode.getObject() != null && dbNode.getObject().getDataSource() != null) {
                    final DBSDataSourceContainer dsContainer = dbNode.getObject().getDataSource().getContainer();
                    createPathRow(
                        infoGroup,
                        dsContainer.getDriver().getIcon(),
                        "Driver",
                        dsContainer.getDriver().getName(),
                        new SelectionAdapter() {
                            public void widgetSelected(SelectionEvent e)
                            {
                                DriverEditDialog dialog = new DriverEditDialog(getSite().getShell(), (DriverDescriptor) dsContainer.getDriver());
                                dialog.open();
                            }
                        });
                }
            }
*/
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
                databaseNode.getMeta().getItemLabel(),
                databaseNode.getNodeName(),
                databaseNode == node ? null : new SelectionAdapter() {
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

        PropertySourceEditable propertySource = new PropertySourceEditable(
            getEditorInput().getCommandContext(),
            getEditorInput().getTreeNode(),
            getEditorInput().getDatabaseObject());
        propertySource.collectProperties();
        properties.selectionChanged(this, new StructuredSelection(propertySource));

        final String folderId = getEditorInput().getDefaultFolderId();
        if (folderId != null) {
            properties.setSelectedTab(folderId);
        }

        properties.addTabSelectionListener(new ITabSelectionListener() {
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
        // Remove lazy props listener
        //PropertiesContributor.getInstance().removeLazyListener(this);

        if (properties != null) {
            properties.dispose();
            properties = null;
        }

        super.dispose();
    }

    public void setFocus()
    {
        properties.setFocus();
    }

    public void doSave(IProgressMonitor monitor)
    {

    }

    public void doSaveAs()
    {
    }

    public void init(IEditorSite site, IEditorInput input)
        throws PartInitException
    {
        setSite(site);
        setInput(input);
    }

    public boolean isDirty()
    {
        return false;
    }

    public boolean isSaveAsAllowed()
    {
        return false;
    }

    public void refreshPart(Object source) {
        if (properties != null) {
            properties.refresh();
        }
    }

    private void createPathRow(Composite infoGroup, Image image, String label, String value, SelectionListener selectionListener)
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

    public ProgressPageControl getProgressControl()
    {
        return pageControl;
    }

    public Object getActiveFolder()
    {
        TabContents currentTab = properties.getCurrentTab();
        if (currentTab != null) {
            ISection[] sections = currentTab.getSections();
            if (CommonUtils.isEmpty(sections)) {
                return null;
            } else if (sections.length == 1) {
                return sections[0];
            } else {
                return sections;
            }
        }
        return null;
    }

    public void switchFolder(String folderId)
    {
        properties.setSelectedTab(folderId);
    }

    public void addFolderListener(IFolderListener listener)
    {
        synchronized (folderListeners) {
            folderListeners.add(listener);
        }
    }

    public void removeFolderListener(IFolderListener listener)
    {
        synchronized (folderListeners) {
            folderListeners.remove(listener);
        }
    }

    private ISearchContextProvider getFolderSearch()
    {
        Object activeFolder = getActiveFolder();
        if (activeFolder instanceof ISearchContextProvider) {
            return (ISearchContextProvider)activeFolder;
        }
        return null;
    }

    public boolean isSearchPossible()
    {
        return true;
    }

    public boolean isSearchEnabled()
    {
        ISearchContextProvider provider = getFolderSearch();
        return provider != null && provider.isSearchEnabled();
    }

    public boolean performSearch(SearchType searchType)
    {
        return getFolderSearch().performSearch(searchType);
    }
}