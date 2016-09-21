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
package org.jkiss.dbeaver.ui.controls.resultset.panel;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * RSV value view panel
 */
public class MetaDataPanel implements IResultSetPanel {

    private static final Log log = Log.getLog(MetaDataPanel.class);

    public static final String PANEL_ID = "results-metadata";

    private IResultSetPresentation presentation;
    private MetaDataTable attributeList;
    private List<DBDAttributeBinding> curAttributes;

    public MetaDataPanel() {
    }

    @Override
    public String getPanelTitle() {
        return "MetaData";
    }

    @Override
    public DBPImage getPanelImage() {
        return UIIcon.PANEL_METADATA;
    }

    @Override
    public String getPanelDescription() {
        return "Resultset metadata";
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;

        this.attributeList = new MetaDataTable(parent);
        this.attributeList.setFitWidth(false);
        this.attributeList.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                IStructuredSelection selection = attributeList.getItemsViewer().getStructuredSelection();
                if (!selection.isEmpty()) {
                    DBDAttributeBinding attr = (DBDAttributeBinding) selection.getFirstElement();
                    if (attr != null) {
                        if (isAttributeVisible(attr)) {
                            presentation.setCurrentAttribute(attr);
                        }
                    }
                }
            }
        });

        return this.attributeList;
    }

    private boolean isAttributeVisible(DBDAttributeBinding attr) {
        return presentation.getController().getModel().getVisibleAttributes().contains(attr);
    }

    @Override
    public void activatePanel() {
        refresh();
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh() {
        if (attributeList.isLoading()) {
            return;
        }
        List<DBDAttributeBinding> newAttributes = Arrays.asList(presentation.getController().getModel().getAttributes());
        if (CommonUtils.equalObjects(curAttributes, newAttributes)) {
            // No changes
            return;
        }
        curAttributes = newAttributes;
        Control table = attributeList.getControl();
        table.setRedraw(false);
        try {
            attributeList.clearListData();
            attributeList.loadData();
        } finally {
            table.setRedraw(true);
        }
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
    }

    private class MetaDataTable extends DatabaseObjectListControl<DBDAttributeBinding> {
        protected MetaDataTable(Composite parent) {
            super(parent, SWT.SHEET, new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    List<DBDAttributeBinding> nested = ((DBDAttributeBinding) parentElement).getNestedBindings();
                    return nested == null ? new Object[0] : nested.toArray(new Object[nested.size()]);
                }

                @Override
                public boolean hasChildren(Object element) {
                    return !CommonUtils.isEmpty(((DBDAttributeBinding) element).getNestedBindings());
                }
            });
        }

        @Override
        protected Object getObjectValue(DBDAttributeBinding item) {
            if (item instanceof DBDAttributeBindingMeta) {
                return item.getMetaAttribute();
            } else {
                return item.getEntityAttribute();
            }
        }

        @Nullable
        @Override
        protected DBPImage getObjectImage(DBDAttributeBinding item) {
            return DBUtils.getTypeImage(item.getMetaAttribute());
        }

        @Override
        protected Color getObjectForeground(DBDAttributeBinding item) {
            if (item.getParentObject() == null && !isAttributeVisible(item)) {
                return presentation.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
            }
            return super.getObjectForeground(item);
        }

        @Override
        protected LoadingJob<Collection<DBDAttributeBinding>> createLoadService() {
            return LoadingJob.createService(
                new LoadAttributesService(),
                new ObjectsLoadVisualizer()
                {
                    @Override
                    public void completeLoading(Collection<DBDAttributeBinding> items) {
                        super.completeLoading(items);
                        ((TreeViewer)attributeList.getItemsViewer()).expandToLevel(2);
                    }
                });
        }
    }

    private class LoadAttributesService extends DatabaseLoadService<Collection<DBDAttributeBinding>> {

        protected LoadAttributesService()
        {
            super("Load sessions", presentation.getController().getExecutionContext());
        }

        @Override
        public Collection<DBDAttributeBinding> evaluate()
            throws InvocationTargetException, InterruptedException
        {
            return curAttributes;
        }
    }
}
