/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.entity;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.ui.actions.navigator.NavigatorHandlerObjectOpen;
import org.jkiss.dbeaver.ui.views.properties.PropertyPageStandard;

import java.util.ArrayList;
import java.util.List;

/**
 * DefaultObjectEditor
 */
public class DefaultObjectEditor extends EditorPart implements IRefreshablePart
{
    static final Log log = LogFactory.getLog(DefaultObjectEditor.class);

    private PropertyPageStandard properties;

    public DefaultObjectEditor()
    {
    }

    public void createPartControl(Composite parent)
    {
        EntityEditorInput entityInput = (EntityEditorInput) getEditorInput();
        DBNNode node = entityInput.getTreeNode();

        Composite container = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(1, true);
        container.setLayout(gl);

        {
            Group infoGroup = new Group(container, SWT.NONE);
            infoGroup.setText("Information");
            gl = new GridLayout(3, false);
            infoGroup.setLayout(gl);

            List<DBNDatabaseNode> nodeList = new ArrayList<DBNDatabaseNode>();
            for (DBNNode n = node; n != null; n = n.getParentNode()) {
                if (n instanceof DBNDatabaseNode && !(n instanceof DBNDatabaseFolder)) {
                    nodeList.add(0, (DBNDatabaseNode)n);
                }
            }
            for (final DBNDatabaseNode databaseNode : nodeList) {
                Label objectIcon = new Label(infoGroup, SWT.NONE);
                objectIcon.setImage(databaseNode.getNodeIconDefault());

                Label objectLabel = new Label(infoGroup, SWT.NONE);
                objectLabel.setText(databaseNode.getMeta().getItemLabel() + ":");

                Link objectLink = new Link(infoGroup, SWT.NONE);
                //Text objectText = new Text(infoGroup, SWT.BORDER);
                GridData gd = new GridData(GridData.FILL_HORIZONTAL);
                objectLink.setLayoutData(gd);

                if (databaseNode == node) {
                    objectLink.setText(databaseNode.getNodeName());
                } else {
                    objectLink.setText("<A>" + databaseNode.getNodeName() + "</A>");
                    objectLink.addSelectionListener(new SelectionAdapter()
                    {
                        public void widgetSelected(SelectionEvent e)
                        {
                            NavigatorHandlerObjectOpen.openEntityEditor(databaseNode, null, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
                        }
                    });
                    objectLink.setToolTipText("Open '" + databaseNode.getNodeName() + "' viewer");
                }
            }
        }

        {
            Group propsGroup = new Group(container, SWT.NONE);
            propsGroup.setText("Properties");
            gl = new GridLayout(2, false);
            propsGroup.setLayout(gl);
            GridData gd = new GridData(GridData.FILL_BOTH);
            propsGroup.setLayoutData(gd);

            DBNNode itemObject = entityInput.getTreeNode();
            //final PropertyCollector propertyCollector = new PropertyCollector(itemObject);
            //List<PropertyAnnoDescriptor> annoProps = PropertyAnnoDescriptor.extractAnnotations(itemObject);

            properties = new PropertyPageStandard();
            //propertiesView.
            properties.createControl(propsGroup);
            gd = new GridData(GridData.FILL_BOTH);
            //gd.heightHint = 100;
            properties.getControl().setLayoutData(gd);
            if (itemObject != null) {
                properties.setCurrentObject(this, itemObject);
            }
        }
    }

    @Override
    public void dispose()
    {
        if (properties != null) {
            properties.dispose();
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
}