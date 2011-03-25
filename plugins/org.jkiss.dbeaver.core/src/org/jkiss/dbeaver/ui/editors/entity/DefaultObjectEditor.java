/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import net.sf.jkiss.utils.CommonUtils;
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
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DriverDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.dialogs.driver.DriverEditDialog;
import org.jkiss.dbeaver.ui.views.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.ui.views.properties.PropertiesContributor;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageTabbed;
import org.jkiss.dbeaver.ui.views.properties.ProxyPageSite;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultObjectEditor
 */
public class DefaultObjectEditor extends EditorPart implements IRefreshablePart, ILazyPropertyLoadListener
{
    static final Log log = LogFactory.getLog(DefaultObjectEditor.class);

    private PropertyPageTabbed properties;
    private Text nameText;
    private Text descriptionText;

    public DefaultObjectEditor()
    {
    }

    private DBNNode getTreeNode()
    {
        EntityEditorInput entityInput = (EntityEditorInput) getEditorInput();
        return entityInput.getTreeNode();
    }

    public void createPartControl(Composite parent)
    {
        // Add lazy props listener
        PropertiesContributor.getInstance().addLazyListener(this);

        DBNNode node = getTreeNode();

        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(2, false));

        if (node == null) {
            return;
        }
        {
            // Path
            Group infoGroup = UIUtils.createControlGroup(container, "Path", 3, GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);

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
            List<DBNDatabaseNode> nodeList = new ArrayList<DBNDatabaseNode>();
            for (DBNNode n = node.getParentNode(); n != null; n = n.getParentNode()) {
                if (n instanceof DBNDatabaseNode && !(n instanceof DBNDatabaseFolder)) {
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
        {
            // General options
            Group infoGroup = UIUtils.createControlGroup(container, "General", 2, GridData.FILL_HORIZONTAL | GridData.HORIZONTAL_ALIGN_BEGINNING | GridData.VERTICAL_ALIGN_BEGINNING, 0);
            UIUtils.createControlLabel(infoGroup, "Name");
            nameText = new Text(infoGroup, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.widthHint = 200;
            nameText.setLayoutData(gd);
            nameText.setText(node.getNodeName());
            nameText.setEditable(false);

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
            descriptionText.setEditable(false);
        }

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
                properties.selectionChanged(this, new StructuredSelection(itemObject));
            }
        }
    }

    @Override
    public void dispose()
    {
        // Add lazy props listener
        PropertiesContributor.getInstance().removeLazyListener(this);

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
        UIUtils.createControlLabel(infoGroup, label);

        Link objectLink = new Link(infoGroup, SWT.NONE);
        objectLink.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        if (selectionListener == null) {
            objectLink.setText(value);
        } else {
            objectLink.setText("<A>" + value + "</A>");
            objectLink.addSelectionListener(selectionListener);
        }
    }

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
}