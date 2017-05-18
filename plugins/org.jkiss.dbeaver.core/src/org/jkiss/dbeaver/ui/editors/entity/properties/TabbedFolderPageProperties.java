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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.*;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

/**
 * TabbedFolderPageProperties
 */
public class TabbedFolderPageProperties extends TabbedFolderPage implements ILazyPropertyLoadListener, IRefreshablePart, DBPEventListener {

    protected IWorkbenchPart part;
    protected IDatabaseEditorInput input;
	protected PropertyTreeViewer propertyTree;
    private Font boldFont;
    private UIJob refreshJob = null;
    private DBPPropertySource curPropertySource;

    public TabbedFolderPageProperties(IWorkbenchPart part, IDatabaseEditorInput input) {
        this.part = part;
        this.input = input;
    }

    @Override
    public void createControl(Composite parent)
    {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

		propertyTree = new PropertyTreeViewer(parent, SWT.NONE);
        propertyTree.setExtraLabelProvider(new PropertyLabelProvider());
        propertyTree.setExpandMode(PropertyTreeViewer.ExpandMode.FIRST);
        propertyTree.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IWorkbenchPartSite site = part.getSite();
                IActionBars actionBars = null;
                if (site instanceof IEditorSite) {
                    actionBars = ((IEditorSite) site).getActionBars();
                } else if (site instanceof IViewSite) {
                    actionBars = ((IViewSite) site).getActionBars();
                }
                if (actionBars != null) {
                    String statusText = null;
                    Object selection = propertyTree.getStructuredSelection().getFirstElement();
                    DBPPropertyDescriptor prop = propertyTree.getPropertyFromElement(selection);
                    if (prop != null) {
                        statusText = prop.getDescription();
                        if (CommonUtils.isEmpty(statusText)) {
                            statusText = prop.getDisplayName();
                        }
                    }
                    if (CommonUtils.isEmpty(statusText)) {
                        statusText = CommonUtils.toString(selection);
                    }
                    actionBars.getStatusLineManager().setMessage(CommonUtils.notEmpty(statusText));
                }
            }
        });
        PropertiesContributor.getInstance().addLazyListener(this);

        curPropertySource = input.getPropertySource();
        propertyTree.loadProperties(curPropertySource);

        if (input.getDatabaseObject() != null) {
            DBUtils.getObjectRegistry((DBSObject) curPropertySource.getEditableValue()).addDataSourceListener(this);
        }
        propertyTree.getControl().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });
	}

    @Override
    public void setFocus() {
        propertyTree.getControl().setFocus();
    }

    @Override
    public void dispose() {
        if (curPropertySource.getEditableValue() instanceof DBSObject) {
            DBUtils.getObjectRegistry((DBSObject) curPropertySource.getEditableValue()).removeDataSourceListener(this);
        }
        UIUtils.dispose(boldFont);
        PropertiesContributor.getInstance().removeLazyListener(this);
		super.dispose();
	}

    @Override
    public void handlePropertyLoad(Object object, DBPPropertyDescriptor property, Object propertyValue, boolean completed)
    {
        if (curPropertySource.getEditableValue() == object && !propertyTree.getControl().isDisposed()) {
            refreshProperties();
        }
    }

    private void refreshProperties()
    {
        synchronized (this) {
            if (refreshJob == null) {
                refreshJob = new RefreshJob();
                refreshJob.schedule(100);
            }
        }
    }

    @Override
    public void handleDataSourceEvent(DBPEvent event)
    {
        if (input.getDatabaseObject() == event.getObject() && !Boolean.FALSE.equals(event.getEnabled()) && !propertyTree.getControl().isDisposed()) {
            refreshProperties();
        }
    }

    @Override
    public void refreshPart(Object source, boolean force) {
        if (force) {
            curPropertySource = input.getPropertySource();
            propertyTree.loadProperties(curPropertySource);
            refreshProperties();
        }
    }

    private class PropertyLabelProvider extends ColumnLabelProvider implements IFontProvider {
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof DBPPropertyDescriptor && curPropertySource != null && ((DBPPropertyDescriptor) element).isEditable(curPropertySource.getEditableValue())) {
                return boldFont;
            }
            return null;
        }
    }

    private class RefreshJob extends UIJob {
        public RefreshJob()
        {
            super("Refresh properties");
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor)
        {
            if (!propertyTree.getControl().isDisposed()) {
                propertyTree.refresh();
                // Force control redraw (to repaint hyperlinks and other stuff)
                propertyTree.getControl().redraw();
            }

            synchronized (TabbedFolderPageProperties.this) {
                refreshJob = null;
            }
            return Status.OK_STATUS;
        }
    }

}