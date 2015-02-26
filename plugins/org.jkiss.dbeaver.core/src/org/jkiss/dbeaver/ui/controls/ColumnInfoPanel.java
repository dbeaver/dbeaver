/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.ui.controls;

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
import org.jkiss.dbeaver.ui.properties.PropertyCollector;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;

import java.util.ArrayList;
import java.util.List;

/**
 * Column info panel.
 */
public class ColumnInfoPanel extends Composite {

    public ColumnInfoPanel(Composite parent, int style, DBDValueController valueController) {
        super(parent, style);
        if (valueController instanceof DBDAttributeController) {
            this.createPanel((DBDAttributeController) valueController);
        }
    }

    protected void createPanel(DBDAttributeController valueController)
    {
        PropertyCollector infoItem = new PropertyCollector(valueController.getBinding().getMetaAttribute(), false);
        infoItem.collectProperties();
        valueController.getValueHandler().contributeProperties(infoItem, valueController);
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
        private final DBDRowController rowController;
        @NotNull
        private final DBDRowIdentifier rowIdentifier;

        public CellKeyInfo(@NotNull DBDRowController rowController, @NotNull DBDRowIdentifier rowIdentifier) {
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
            List<KeyColumnValue> columns = new ArrayList<KeyColumnValue>();
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
