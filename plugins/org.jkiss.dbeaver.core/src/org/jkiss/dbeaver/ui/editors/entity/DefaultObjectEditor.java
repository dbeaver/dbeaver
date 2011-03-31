/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.edit.DBEObjectCommander;
import org.jkiss.dbeaver.model.edit.DBEObjectDescriber;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.controls.ObjectEditorPageControl;
import org.jkiss.dbeaver.ui.editors.AbstractDatabaseObjectEditor;
import org.jkiss.dbeaver.ui.views.properties.IPropertyFilter;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.ui.views.properties.PropertySourceEditable;
import org.jkiss.dbeaver.ui.views.properties.ProxyPageSite;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultObjectEditor
 */
public class DefaultObjectEditor extends AbstractDatabaseObjectEditor implements IRefreshablePart//, ILazyPropertyLoadListener
{
    static final Log log = LogFactory.getLog(DefaultObjectEditor.class);

    private PropertyPageTabbed properties;
    //private Text nameText;
    //private Text descriptionText;

    public DefaultObjectEditor()
    {
    }

    private DBNNode getTreeNode()
    {
        return getEditorInput().getTreeNode();
    }

    public void createPartControl(Composite parent)
    {
        // Add lazy props listener
        //PropertiesContributor.getInstance().addLazyListener(this);

        ObjectEditorPageControl pageControl = new ObjectEditorPageControl(parent, SWT.NONE, this);

        DBNNode node = getTreeNode();

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
        {
            // Path
            Group infoGroup = UIUtils.createControlGroup(container, "Path", 3, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
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

        createNamePanel(node, container);

        {
            // Properties
            Composite propsPlaceholder = new Composite(container, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.horizontalSpan = 2;
            propsPlaceholder.setLayoutData(gd);
            propsPlaceholder.setLayout(new FormLayout());

            DBNNode itemObject = getTreeNode();
            //final PropertyCollector propertyCollector = new PropertyCollector(itemObject);
            //List<ObjectPropertyDescriptor> annoProps = ObjectPropertyDescriptor.extractAnnotations(itemObject);

            properties = new PropertyPageTabbed();
            properties.init(new ProxyPageSite(getSite()));
            properties.createControl(propsPlaceholder);
            if (itemObject != null) {
                DBEObjectCommander commander = getEditorInput().getObjectCommander();
                PropertySourceEditable propertySource = new PropertySourceEditable(
                    itemObject instanceof DBNDatabaseNode ? ((DBNDatabaseNode) itemObject).getObject() : itemObject,
                    commander);
                propertySource.collectProperties(new IPropertyFilter() {
                    public boolean isValid(IPropertyDescriptor property)
                    {
                        //if (property.get)
                        return true;
                    }
                });
                properties.selectionChanged(this, new StructuredSelection(propertySource));
            }
        }

        pageControl.createProgressPanel();
    }

    private void createNamePanel(DBNNode node, Composite container)
    {
/*
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
*/
    }

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

}