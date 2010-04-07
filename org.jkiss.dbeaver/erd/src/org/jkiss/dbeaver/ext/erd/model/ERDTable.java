package org.jkiss.dbeaver.ext.erd.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSConstraint;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSStructureContainer;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERDTable
 */
public class ERDTable extends ERDNode {

    private DBSTable table;
    private List<ERDTableColumn> columns = new ArrayList<ERDTableColumn>();

    private int tableRectWidth, tableRectHeight;
    private boolean independent;

    public ERDTable(DBSTable table)
    {
        super(table);
        this.table = table;
    }

    public String getName()
    {
        return table.getName();
    }

    public DBSTable<DBPDataSource, DBSStructureContainer<DBPDataSource>> getTable()
    {
        return table;
    }

    public List<ERDTableColumn> getColumns()
    {
        return columns;
    }

    public int getRectWidth()
    {
        return tableRectWidth;
    }

    public int getRectHeight()
    {
        return tableRectHeight;
    }

    /**
     * Independent table do not have FK columns in it's PK
     * @return true or false
     */
    public boolean isIndependent()
    {
        return independent;
    }

    private boolean isInForeignKey(DBSTableColumn tableColumn)
        throws DBException
    {
        Collection<? extends DBSForeignKey> constraints = getTable().getImportedKeys();
        if (constraints != null) {
            for (DBSConstraint constraint : constraints) {
                if (constraint.getColumn(tableColumn) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    DBSConstraint getPrimaryKey()
        throws DBException
    {
        // Try to get key number from primary key constraint column
        Collection<? extends DBSConstraint> constraints = getTable().getConstraints();
        if (constraints != null) {
            for (DBSConstraint constraint : constraints) {
                if (constraint.getConstraintType() == DBSConstraintType.PRIMARY_KEY) {
                    return constraint;
                }
            }
        }
        return null;
    }

    public void calculateContent(Graphics graphics)
        throws DBException
    {
        FontMetrics titleMetrics = graphics.getFontMetrics(ERDConstants.ENTITY_NAME_FONT);
        FontMetrics attrMetrics = graphics.getFontMetrics(ERDConstants.ENTITY_ATTR_FONT);

        // Get all columns
        Collection<? extends DBSTableColumn> dbsTableColumns = getTable().getColumns();
        for (DBSTableColumn column : dbsTableColumns) {
            ERDTableColumn erdColumn = new ERDTableColumn(this, column);
            columns.add(erdColumn);
            int columnStrWidth = attrMetrics.stringWidth(erdColumn.getName());
            if (columnStrWidth > tableRectWidth) {
                tableRectWidth = columnStrWidth;
            }
        }
        tableRectWidth += 10; // Borders

        // Calculate rect height
        tableRectHeight = titleMetrics.getHeight();
        tableRectHeight += 10; // Borders + divider
        int totalAttrs = columns.size();
        if (totalAttrs < 2) {
            totalAttrs = 2;
        }
        tableRectHeight += totalAttrs * attrMetrics.getHeight();

        // Order columns
        Collections.sort(columns);

        // check independence
        independent = true;
        DBSConstraint primaryKey = getPrimaryKey();
        if (primaryKey != null) {
            Collection<DBSConstraintColumn> constrColumns = primaryKey.getColumns();
            for (DBSConstraintColumn constrCol : constrColumns) {
                if (isInForeignKey(constrCol.getTableColumn())) {
                    independent = false;
                }
            }
        }
    }

    public String getTipString()
    {
        return getName();
    }

    @Override
    public String getId()
    {
        return table.getFullQualifiedName();
    }
}
