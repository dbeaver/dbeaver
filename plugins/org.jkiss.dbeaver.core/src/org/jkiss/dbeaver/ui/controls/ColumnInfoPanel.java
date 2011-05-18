/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
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
        this.createPanel(valueController);
    }

    protected void createPanel(DBDValueController valueController)
    {
        PropertyCollector infoItem = new PropertyCollector(valueController.getColumnMetaData(), false);
        infoItem.addProperty("Table_Name", "Table Name", valueController.getColumnMetaData().getTableName());
        infoItem.addProperty("Column_Name", "Column Name", valueController.getColumnMetaData().getName() );
        infoItem.addProperty("Column_Type", "Column Type", valueController.getColumnMetaData().getTypeName() );
        valueController.getValueHandler().fillProperties(infoItem, valueController);
        if (valueController.getValueLocator() != null) {
            infoItem.addProperty("Key", "Key", new CellKeyInfo(valueController) );
        }

        GridData gd = new GridData(GridData.FILL_BOTH);
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
        public String getName()
        {
            return column.getName();
        }
        public String toString()
        {
            return value == null ? "[NULL]" : value.toString();
        }
    }

    public static class CellKeyInfo implements DBPObject {
        private DBDValueController valueController;

        private CellKeyInfo(DBDValueController valueController)
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
