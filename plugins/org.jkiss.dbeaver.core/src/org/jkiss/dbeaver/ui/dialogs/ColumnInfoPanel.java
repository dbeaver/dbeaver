/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.data.IAttributeController;
import org.jkiss.dbeaver.ui.data.IRowController;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Column info panel.
 */
public class ColumnInfoPanel extends Composite {

    public ColumnInfoPanel(Composite parent, int style, IValueController valueController) {
        super(parent, style);
        if (valueController instanceof IAttributeController) {
            this.createPanel((IAttributeController) valueController);
        }
    }

    protected void createPanel(IAttributeController valueController)
    {
        PropertyCollector infoItem = new PropertyCollector(valueController.getBinding().getMetaAttribute(), false);
        infoItem.collectProperties();
        valueController.getValueManager().contributeProperties(infoItem, valueController);
        DBDRowIdentifier rowIdentifier = valueController.getRowIdentifier();
        if (rowIdentifier != null) {
            infoItem.addProperty(
                null,
                "Key",
                CoreMessages.controls_column_info_panel_property_key,
                new CellKeyInfo(valueController.getRowController(), rowIdentifier)
            );
        }

        this.setLayout(new FillLayout());
        {
            PropertyTreeViewer propViewer = new PropertyTreeViewer(this, SWT.H_SCROLL | SWT.V_SCROLL);
            propViewer.loadProperties(infoItem);
        }
    }

    public static class KeyColumnValue implements DBPNamedObject {
        private DBDAttributeBinding attribute;
        private Object value;
        public KeyColumnValue(DBDAttributeBinding attribute, @Nullable Object value)
        {
            this.attribute = attribute;
            this.value = value;
        }
        @NotNull
        @Override
        public String getName()
        {
            return attribute.getName();
        }
        @Override
        public String toString()
        {
            return DBUtils.isNullValue(value) ? DBConstants.NULL_VALUE_LABEL : value.toString(); //$NON-NLS-1$
        }
    }

    public static class CellKeyInfo implements DBPObject {
        @NotNull
        private final IRowController rowController;
        @NotNull
        private final DBDRowIdentifier rowIdentifier;

        public CellKeyInfo(@NotNull IRowController rowController, @NotNull DBDRowIdentifier rowIdentifier) {
            this.rowController = rowController;
            this.rowIdentifier = rowIdentifier;
        }

        @Property(viewable = true, order = 1, category = "general")
        public String getType()
        {
            return rowIdentifier.getKeyType();
        }

        @Property(viewable = true, order = 2, category = "general")
        public String getName()
        {
            return rowIdentifier.getUniqueKey().getName();
        }

        @Property(viewable = true, order = 3, category = "columns")
        public List<KeyColumnValue> getColumns()
        {
            List<DBDAttributeBinding> rowAttributes = rowController.getRowAttributes();
            List<KeyColumnValue> columns = new ArrayList<>();
            for (DBDAttributeBinding binding : rowIdentifier.getAttributes()) {
                columns.add(new KeyColumnValue(binding, rowController.getAttributeValue(binding)));
            }
            return columns;
        }

        @Override
        public String toString()
        {
            return "";//valueController.getRowIdentifier().getEntityIdentifier()
        }
    }

}
