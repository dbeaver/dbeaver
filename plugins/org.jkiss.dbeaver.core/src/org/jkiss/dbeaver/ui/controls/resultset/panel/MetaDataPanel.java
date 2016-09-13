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
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.ViewerColumnController;
import org.jkiss.dbeaver.ui.controls.TreeContentProvider;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPanel;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetPresentation;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * RSV value view panel
 */
public class MetaDataPanel implements IResultSetPanel {

    private static final Log log = Log.getLog(MetaDataPanel.class);

    public static final String PANEL_ID = "results-metadata";

    private IResultSetPresentation presentation;
    private TreeViewer attributeList;
    private ViewerColumnController attrController;

    private enum AttributeProperty {
        NAME("Attribute", "Attribute name", true, true),
        LABEL("Label", "Attribute label", false, false),
        POSITION("#", "Ordinal position", true, true),
        TYPE("Type", "Data type name", true, true),
        TYPE_ID("Type ID", "Data type ID", false, false),
        LENGTH("Length", "Max length", true, false),

        PRECISION("Precision", "Precision", false, false),
        SCALE("Scale", "Scale", false, false),

        TABLE("Table", "Table name", true, false),
        SCHEMA("Schema", "Table schema name", false, false),
        CATALOG("Catalog", "Table catalog name", false, false),
        ;

        private final String title;
        private final String description;
        private final boolean isDefault;
        private final boolean isRequired;

        AttributeProperty(String title, String description, boolean isDefault, boolean isRequired) {
            this.title = title;
            this.description = description;
            this.isDefault = isDefault;
            this.isRequired = isRequired;
        }
    }

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

        this.attributeList = new TreeViewer(parent, SWT.FULL_SELECTION);
        this.attributeList.getTree().setHeaderVisible(true);
        this.attributeList.getTree().setLinesVisible(true);
        this.attrController = new ViewerColumnController(PANEL_ID, this.attributeList);
        for (AttributeProperty prop : AttributeProperty.values()) {
            attrController.addColumn(
                prop.title,
                prop.description,
                SWT.LEFT,
                prop.isDefault,
                prop.isRequired,
                new PropertyLabelProvider(prop));
        }
        attrController.createColumns();
        this.attributeList.setContentProvider(new TreeContentProvider() {
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

        return this.attributeList.getTree();
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
        Tree table = attributeList.getTree();
        table.setRedraw(false);
        try {
            attributeList.setInput(presentation.getController().getModel().getVisibleAttributes());
            attributeList.expandAll();
            for (TreeColumn column : table.getColumns()) {
                column.pack();
            }
            //UIUtils.packColumns(table, false);
            //this.attrController.repackColumns();
        } finally {
            table.setRedraw(true);
        }
    }

    @Override
    public void contributeActions(ToolBarManager manager) {
        manager.add(new Action("Configure columns", DBeaverIcons.getImageDescriptor(UIIcon.PROPERTIES)) {
            @Override
            public void run() {
                attrController.configureColumns();
            }
        });
    }

    private class PropertyLabelProvider extends ColumnLabelProvider {
        private final AttributeProperty property;

        private PropertyLabelProvider(AttributeProperty property) {
            this.property = property;
        }

        @Override
        public Image getImage(Object element) {
            if (property == AttributeProperty.NAME) {
                return DBeaverIcons.getImage(DBUtils.getTypeImage(((DBDAttributeBinding) element).getMetaAttribute()));
            }
            return null;
        }

        @Override
        public String getText(Object element) {
            DBDAttributeBinding binding = (DBDAttributeBinding) element;
            switch (property) {
                case NAME:
                    return binding.getMetaAttribute().getName();
                case LABEL:
                    return binding.getMetaAttribute().getLabel();
                case POSITION:
                    return String.valueOf(binding.getMetaAttribute().getOrdinalPosition());
                case TYPE:
                    return binding.getMetaAttribute().getTypeName();
                case TYPE_ID:
                    return String.valueOf(binding.getMetaAttribute().getTypeID());
                case LENGTH:
                    return String.valueOf(binding.getMetaAttribute().getMaxLength());
                case PRECISION:
                    return String.valueOf(binding.getMetaAttribute().getPrecision());
                case SCALE:
                    return String.valueOf(binding.getMetaAttribute().getScale());
                case TABLE:
                    return binding.getMetaAttribute().getEntityName();
            }
            return null;
        }
    }
}
