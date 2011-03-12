/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

/*
 * Created on Jul 13, 2004
 */
package org.jkiss.dbeaver.ext.erd.model;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.xml.XMLBuilder;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSForeignKey;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTable;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * Represents a Schema in the model. Note that this class also includes
 * diagram specific information (layoutManualDesired and layoutManualAllowed fields)
 * although ideally these should be in a separate model hierarchy
 * @author Serge Rieder
 */
public class EntityDiagram extends ERDObject<DBSObject>
{

	private String name;
	private List<ERDTable> tables = new ArrayList<ERDTable>();
	private boolean layoutManualDesired = true;
	private boolean layoutManualAllowed = false;

	public EntityDiagram(DBSObject container, String name)
	{
		super(container);
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		this.name = name;
	}

	public synchronized void addTable(ERDTable table, boolean reflect)
	{
		tables.add(table);
        if (reflect) {
		    firePropertyChange(CHILD, null, table);
        }
	}

	public synchronized void addTable(ERDTable table, int i, boolean reflect)
	{
		tables.add(i, table);
        if (reflect) {
		    firePropertyChange(CHILD, null, table);
        }
	}

	public synchronized void removeTable(ERDTable table, boolean reflect)
	{
		tables.remove(table);
        if (reflect) {
		    firePropertyChange(CHILD, table, null);
        }
	}

    /**
	 * @return the Tables for the current schema
	 */
	public synchronized List<ERDTable> getTables()
	{
		return tables;
	}

	/**
	 * @return the name of the schema
	 */
	public String getName()
	{
		return name;
	}

    public void setName(String name)
    {
        this.name = name;
    }

	/**
	 * @param layoutManualAllowed
	 *            The layoutManualAllowed to set.
	 */
	public void setLayoutManualAllowed(boolean layoutManualAllowed)
	{
		this.layoutManualAllowed = layoutManualAllowed;
	}

	/**
	 * @return Returns the layoutManualDesired.
	 */
	public boolean isLayoutManualDesired()
	{
		return layoutManualDesired;
	}

	/**
	 * @param layoutManualDesired
	 *            The layoutManualDesired to set.
	 */
	public void setLayoutManualDesired(boolean layoutManualDesired)
	{
		this.layoutManualDesired = layoutManualDesired;
	}

	/**
	 * @return Returns whether we can lay out individual tables manually using the XYLayout
	 */
	public boolean isLayoutManualAllowed()
	{
		return layoutManualAllowed;
	}

    public int getEntityCount() {
        return tables.size();
    }

    public void load(InputStream in)
        throws IOException
    {

    }

    public void save(OutputStream out)
        throws IOException
    {
        XMLBuilder xml = new XMLBuilder(out, ContentUtils.DEFAULT_FILE_CHARSET);

        xml.startElement("diagram");
        xml.addAttribute("version", 1);
        xml.addAttribute("name", name);

        {
            xml.startElement("entities");
            xml.endElement();
        }
        {
            xml.startElement("relations");
            xml.endElement();
        }
        {
            xml.startElement("notes");
            xml.endElement();
        }

        xml.endElement();

        xml.flush();
    }

    public EntityDiagram copy()
    {
        EntityDiagram copy = new EntityDiagram(getObject(), getName());
        copy.tables.addAll(this.tables);
        copy.layoutManualDesired = this.layoutManualDesired;
        copy.layoutManualAllowed = this.layoutManualAllowed;
        return copy;
    }

    public void fillTables(DBRProgressMonitor monitor, Collection<DBSTable> tables, DBSObject dbObject)
    {
        // Load entities
        Map<DBSTable, ERDTable> tableMap = new HashMap<DBSTable, ERDTable>();
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            ERDTable erdTable = ERDTable.fromObject(monitor, table);
            erdTable.setPrimary(table == dbObject);

            addTable(erdTable, false);
            tableMap.put(table, erdTable);
        }

        // Load relations
        for (DBSTable table : tables) {
            if (monitor.isCanceled()) {
                break;
            }
            ERDTable table1 = tableMap.get(table);
            try {
                Set<DBSTableColumn> fkColumns = new HashSet<DBSTableColumn>();
                // Make associations
                Collection<? extends DBSForeignKey> fks = table.getForeignKeys(monitor);
                for (DBSForeignKey fk : fks) {
                    fkColumns.addAll(DBUtils.getTableColumns(monitor, fk));
                    ERDTable table2 = tableMap.get(fk.getReferencedKey().getTable());
                    if (table2 == null) {
                        //log.warn("Table '" + fk.getReferencedKey().getTable().getFullQualifiedName() + "' not found in ERD");
                    } else {
                        //if (table1 != table2) {
                        new ERDAssociation(fk, table2, table1, false);
                        //}
                    }
                }

                // Mark column's fk flag
                for (ERDTableColumn column : table1.getColumns()) {
                    if (fkColumns.contains(column.getObject())) {
                        column.setInForeignKey(true);
                    }
                }

            } catch (DBException e) {
                log.warn("Could not load table '" + table.getName() + "' foreign keys", e);
            }
        }
    }

    public boolean containsTable(DBSTable table)
    {
        for (ERDTable erdTable : tables) {
            if (erdTable.getObject() == table) {
                return true;
            }
        }
        return false;
    }
}