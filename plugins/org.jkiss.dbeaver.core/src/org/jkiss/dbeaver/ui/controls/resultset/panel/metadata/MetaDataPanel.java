/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.resultset.panel.metadata;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeBindingMeta;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.itemlist.DatabaseObjectListControl;
import org.jkiss.dbeaver.ui.controls.resultset.panel.IResultSetPanel;
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

    public static final String PANEL_ID = "results-metadata";

    private IResultSetPresentation presentation;
    private MetaDataTable attributeList;
    private List<DBDAttributeBinding> curAttributes;
    private Color colorDisabled;
    private transient boolean updateSelection = false;

    public MetaDataPanel() {
    }

    @Override
    public Control createContents(final IResultSetPresentation presentation, Composite parent) {
        this.presentation = presentation;
        this.colorDisabled = presentation.getControl().getDisplay().getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        this.attributeList = new MetaDataTable(parent);
        this.attributeList.setFitWidth(false);
        this.attributeList.getItemsViewer().addSelectionChangedListener(event -> {
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
        });
        if (this.presentation instanceof ISelectionProvider) {
            final ISelectionChangedListener listener = event -> {
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
            };
            ((ISelectionProvider) this.presentation).addSelectionChangedListener(listener);
            attributeList.getControl().addDisposeListener(e ->
                ((ISelectionProvider) presentation).removeSelectionChangedListener(listener));
        }

        return this.attributeList;
    }

    @Override
    public boolean isDirty() {
        return false;
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
        MetaDataTable(Composite parent) {
            super(parent, SWT.SHEET, presentation.getController().getSite(), new TreeContentProvider() {
                @Override
                public Object[] getChildren(Object parentElement) {
                    List<DBDAttributeBinding> nested = ((DBDAttributeBinding) parentElement).getNestedBindings();
                    return nested == null ? new Object[0] : nested.toArray(new Object[0]);
                }

                @Override
                public boolean hasChildren(Object element) {
                    return !CommonUtils.isEmpty(((DBDAttributeBinding) element).getNestedBindings());
                }
            });
        }

        @Override
        public void fillCustomActions(IContributionManager contributionManager) {
            UIUtils.fillDefaultTreeContextMenu(contributionManager, (Tree) getItemsViewer().getControl());
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

        LoadAttributesService()
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
