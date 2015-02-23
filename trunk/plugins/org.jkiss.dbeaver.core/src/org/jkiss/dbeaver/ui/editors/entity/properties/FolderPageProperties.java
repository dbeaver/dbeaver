/*
 * Copyright (C) 2010-2015 Serge Rieder
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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.jkiss.dbeaver.ext.IDatabaseEditorInput;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.FolderPage;
import org.jkiss.dbeaver.ui.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.ui.properties.IPropertyDescriptorEx;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

/**
 * FolderPageProperties
 */
public class FolderPageProperties extends FolderPage implements ILazyPropertyLoadListener, DBPEventListener {

    protected IDatabaseEditorInput input;
	protected PropertyTreeViewer propertyTree;
    private Font boldFont;
    private UIJob refreshJob = null;
    private IPropertySource curPropertySource;

    public FolderPageProperties(IDatabaseEditorInput input) {
        this.input = input;
    }

    @Override
    public void createControl(Composite parent)
    {
        this.boldFont = UIUtils.makeBoldFont(parent.getFont());

		propertyTree = new PropertyTreeViewer(parent, SWT.NONE);
        propertyTree.setExtraLabelProvider(new PropertyLabelProvider());
        PropertiesContributor.getInstance().addLazyListener(this);

        curPropertySource = input.getPropertySource();

        propertyTree.loadProperties(curPropertySource);
        if (curPropertySource.getEditableValue() instanceof DBSObject) {
            DBUtils.getRegistry((DBSObject) curPropertySource.getEditableValue()).addDataSourceListener(this);
        }
	}

	@Override
    public void dispose() {
        if (curPropertySource.getEditableValue() instanceof DBSObject) {
            DBUtils.getRegistry((DBSObject) curPropertySource.getEditableValue()).removeDataSourceListener(this);
        }
        UIUtils.dispose(boldFont);
        PropertiesContributor.getInstance().removeLazyListener(this);
		super.dispose();
	}

    @Override
    public void handlePropertyLoad(Object object, IPropertyDescriptor property, Object propertyValue, boolean completed)
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
        if (curPropertySource.getEditableValue() == event.getObject() && !Boolean.FALSE.equals(event.getEnabled()) && !propertyTree.getControl().isDisposed()) {
            refreshProperties();
        }
    }

    private class PropertyLabelProvider extends ColumnLabelProvider implements IFontProvider {
        @Override
        public Font getFont(Object element)
        {
            if (element instanceof IPropertyDescriptorEx && curPropertySource != null && ((IPropertyDescriptorEx) element).isEditable(curPropertySource.getEditableValue())) {
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

            synchronized (FolderPageProperties.this) {
                refreshJob = null;
            }
            return Status.OK_STATUS;
        }
    }

}