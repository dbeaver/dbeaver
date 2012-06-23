/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.data.DBDColumnController;
import org.jkiss.dbeaver.model.data.DBDRowController;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.exec.DBCColumnMetaData;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.ui.UIUtils;
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
        if (valueController instanceof DBDColumnController) {
            this.createPanel((DBDColumnController) valueController);
        }
    }

    protected void createPanel(DBDColumnController valueController)
    {
        PropertyCollector infoItem = new PropertyCollector(valueController.getColumnMetaData(), false);
        infoItem.addProperty("Table_Name", CoreMessages.controls_column_info_panel_property_table_name, valueController.getColumnMetaData().getTableName()); //$NON-NLS-1$
        infoItem.addProperty("Column_Name", CoreMessages.controls_column_info_panel_property_column_name, valueController.getColumnMetaData().getName() ); //$NON-NLS-1$
        infoItem.addProperty("Column_Type", CoreMessages.controls_column_info_panel_property_column_type, valueController.getColumnMetaData().getTypeName() ); //$NON-NLS-1$
        valueController.getValueHandler().fillProperties(infoItem, valueController);
        if (valueController.getValueLocator() != null) {
            infoItem.addProperty("Key", CoreMessages.controls_column_info_panel_property_key, new CellKeyInfo(valueController) ); //$NON-NLS-1$
        }

        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalIndent = 0;
        gd.verticalIndent = 0;
        this.setLayoutData(gd);
        this.setLayout(new FillLayout());
        {
            Composite ph = UIUtils.createPlaceholder(this, 1);
            ph.setLayout(new FillLayout());
            PropertyTreeViewer propViewer = new PropertyTreeViewer(ph, SWT.NONE);
            propViewer.loadProperties(infoItem);
//            PropertyPageStandard properties = new PropertyPageStandard();
//            properties.init(new ProxyPageSite(valueController.getValueSite()));
//            properties.createControl(ph);
//            properties.selectionChanged(valueController.getValueSite().getPart(), new StructuredSelection(infoItem));
        }
    }

    public static class KeyColumnValue implements DBPNamedObject {
        private DBCColumnMetaData column;
        private Object value;
        public KeyColumnValue(DBCColumnMetaData column, Object value)
        {
            this.column = column;
            this.value = value;
        }
        @Override
        public String getName()
        {
            return column.getName();
        }
        public String toString()
        {
            return value == null ? "[NULL]" : value.toString(); //$NON-NLS-1$
        }
    }

    public static class CellKeyInfo implements DBPObject {
        private DBDColumnController valueController;

        private CellKeyInfo(DBDColumnController valueController)
        {
            this.valueController = valueController;
        }

        @Property(name = "Type", viewable = true, order = 1, category = "general")
        public String getType()
        {
            return valueController.getValueLocator().getKeyType();
        }

        @Property(name = "Name", viewable = true, order = 2, category = "general")
        public String getName()
        {
            return valueController.getValueLocator().getUniqueKey().getName();
        }

        @Property(name = "Columns", viewable = true, order = 3, category = "columns")
        public List<KeyColumnValue> getColumns()
        {
            List<KeyColumnValue> columns = new ArrayList<KeyColumnValue>();
            DBDRowController row = valueController.getRow();
            for (DBCColumnMetaData col : valueController.getValueLocator().getResultSetColumns()) {
                columns.add(new KeyColumnValue(col, row.getColumnValue(col)));
            }
            return columns;
        }

        @Override
        public String toString()
        {
            return valueController.getValueLocator().getKeyKind();
        }
    }

}
