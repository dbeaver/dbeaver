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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.progress.UIJob;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.properties.PropertiesContributor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.FolderPage;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.runtime.properties.ILazyPropertyLoadListener;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

/**
 * FolderPageProperties
 */
public class FolderPageProperties extends FolderPage implements ILazyPropertyLoadListener, DBPEventListener {

    protected IDatabaseEditorInput input;
	protected PropertyTreeViewer propertyTree;
    private Font boldFont;
    private UIJob refreshJob = null;
    private DBPPropertySource curPropertySource;

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
    public void setFocus() {
        propertyTree.getControl().setFocus();
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
        if (curPropertySource.getEditableValue() == event.getObject() && !Boolean.FALSE.equals(event.getEnabled()) && !propertyTree.getControl().isDisposed()) {
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

            synchronized (FolderPageProperties.this) {
                refreshJob = null;
            }
            return Status.OK_STATUS;
        }
    }

}