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

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
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
    private Color colorDisabled;
    private transient boolean updateSelection = false;

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
        this.colorDisabled = presentation.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);

        this.attributeList = new MetaDataTable(parent);
        this.attributeList.setFitWidth(false);
        this.attributeList.getItemsViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                DBDAttributeBinding attr = getSelectedAttribute();
                if (attr != null && !updateSelection) {
                    if (isAttributeVisible(attr)) {
                        updateSelection = true;
                        try {
                            presentation.setCurrentAttribute(attr);
                        } finally {
                            updateSelection = false;
                        }
                    }
                }
            }
        });
        if (this.presentation instanceof ISelectionProvider) {
            final ISelectionChangedListener listener = new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    if (!updateSelection && MetaDataPanel.this.presentation.getController().getVisiblePanel() == MetaDataPanel.this) {
                        DBDAttributeBinding attr = presentation.getCurrentAttribute();
                        if (attr != null && attr != getSelectedAttribute()) {
                            updateSelection = true;
                            try {
                                attributeList.getItemsViewer().setSelection(new StructuredSelection(attr));
                            } finally {
                                updateSelection = false;
                            }
                        }
                    }
                }
            };
            ((ISelectionProvider) this.presentation).addSelectionChangedListener(listener);
            attributeList.getControl().addDisposeListener(new DisposeListener() {
                @Override
                public void widgetDisposed(DisposeEvent e) {
                    ((ISelectionProvider) presentation).removeSelectionChangedListener(listener);
                }
            });
        }

        return this.attributeList;
    }
    private DBDAttributeBinding getSelectedAttribute() {
        IStructuredSelection selection = attributeList.getItemsViewer().getStructuredSelection();
        if (!selection.isEmpty()) {
            return (DBDAttributeBinding) selection.getFirstElement();
        }
        return null;
    }

    private boolean isAttributeVisible(DBDAttributeBinding attr) {
        return presentation.getController().getModel().getVisibleAttributes().contains(attr);
    }

    @Override
    public void activatePanel() {
        refresh(false);
    }

    @Override
    public void deactivatePanel() {

    }

    @Override
    public void refresh(boolean force) {
        if (attributeList.isLoading()) {
            return;
        }
        List<DBDAttributeBinding> newAttributes = Arrays.asList(presentation.getController().getModel().getAttributes());
        if (curAttributes != null && curAttributes.size() == newAttributes.size()) {
            boolean equals = true;
            for (int i = 0; i < curAttributes.size(); i++) {
                if (curAttributes.get(i) != newAttributes.get(i)) {
                    equals = false;
                    break;
                }
            }
            if (equals) {
                // No changes
                return;
            }
        }
        curAttributes = newAttributes;

        attributeList.clearListData();
        attributeList.loadData();
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
    }

    private class MetaDataTable extends DatabaseObjectListControl<DBDAttributeBinding> {
        protected MetaDataTable(Composite parent) {
            super(parent, SWT.SHEET, presentation.getController().getSite(), new TreeContentProvider() {
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
        protected void fillCustomActions(IContributionManager contributionManager) {
            contributionManager.add(new Action("Copy column names") {
                @Override
                public void run() {
                    StringBuilder text = new StringBuilder();
                    for (Object item : getItemsViewer().getStructuredSelection().toArray()) {
                        if (item instanceof DBDAttributeBinding) {
                            if (text.length() > 0) text.append("\n");
                            text.append(((DBDAttributeBinding) item).getName());
                        }
                    }
                    UIUtils.setClipboardContents(getDisplay(), TextTransfer.getInstance(), text.toString());
                }
            });
        }

        @NotNull
        @Override
        protected String getListConfigId(List<Class<?>> classList) {
            final DBCExecutionContext executionContext = presentation.getController().getExecutionContext();
            if (executionContext == null) {
                return "MetaData";
            }
            return "MetaData/" + executionContext.getDataSource().getContainer().getDriver().getId();
        }

        @Override
        protected Object getObjectValue(DBDAttributeBinding item) {
            if (item instanceof DBDAttributeBindingMeta) {
                return item.getMetaAttribute();
            } else {
                return item.getAttribute();
            }
        }

        @Nullable
        @Override
        protected DBPImage getObjectImage(DBDAttributeBinding item) {
            return DBValueFormatting.getObjectImage(item.getMetaAttribute());
        }

        @Override
        protected Color getObjectForeground(DBDAttributeBinding item) {
            if (item.getParentObject() == null && !isAttributeVisible(item)) {
                return colorDisabled;
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
        public Collection<DBDAttributeBinding> evaluate(DBRProgressMonitor monitor)
            throws InvocationTargetException, InterruptedException
        {
            return curAttributes;
        }
    }
}
