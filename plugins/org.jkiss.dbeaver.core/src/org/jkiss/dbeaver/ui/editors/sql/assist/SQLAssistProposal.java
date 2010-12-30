
/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.editors.sql.assist;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.struct.*;

public class SQLAssistProposal {

    public static final int SCHEMA_OBJTYPE = 1; //proposal is a schema
    public static final int TABLE_OBJTYPE = 2; // proposal is a table
    public static final int TABLECOLUMN_OBJTYPE = 3; //proposal is table column
    public final static int CATALOG_OBJTYPE = 4; //reserved
    public final static int FUNCTION_OBJTYPE = 5;
	public final static int STORED_PROCEDURE_OBJTYPE = 6;
	public final static int TRIGGER_OBJTYPE = 7;
	public final static int EVENT_OBJTYPE = 8;
	public final static int TABLEALIAS_OBJTYPE = 9;
    public final static int INDEX_OBJTYPE = 10;
    public final static int SEGMENT_OBJTYPE = 11;
    public static final int UNKNOWN_OBJTYPE = -1;

    private DBSObject fDBObject; // database model object
    private Image fImage = null;
    private String fName = null;
    private String fParentAlias = null;
    private String fParentName = null;
    private String fGrandParentName = null;
    private String fGrandGrandParentName = null;
    private DBSObject fParentObject; //parent of the database model object
    private int fType = UNKNOWN_OBJTYPE;

    public SQLAssistProposal( DBSObject dbObject, String alias ) {
    	this(dbObject);
    	this.fParentAlias = alias;
    }

    /**
     * Constructs an instance of this object to represent the given table alias
     * for purpose of a content assist proposal.
     * @param alias the table alias
     */
    public SQLAssistProposal( String alias ) {
    	fType = TABLEALIAS_OBJTYPE;
        fName = alias;
        fParentName = null;
        fGrandParentName = null;
        fParentObject = null;
        //setImage( SQLEditorResources.getImage( "table_alias" )); //$NON-NLS-1$
    }
    /**
     * Constructs an instance of this object to represent the given database
     * model object for purpose of a content assist proposal.
     * The database object can be one of <code>Schema</code>, <code>Table</code>,
     * <code>Column</code>.
     *
     * @param dbObject
     *            the database model object
     * @see org.eclipse.datatools.modelbase.sql.schema.Schema
     */
    public SQLAssistProposal( DBSObject dbObject ) {
        this.fDBObject = dbObject;
        if (dbObject instanceof DBSSchema) {
            DBSSchema schema = (DBSSchema) dbObject;
            fType = SCHEMA_OBJTYPE;
            fName = schema.getName();
            /* Need to deal with Database vs. Catalog as parent. */
            DBSCatalog db = schema.getCatalog();
            if (db != null) {
                fParentObject = db;
                fParentName = db.getName();
            }
            //setImage( SQLEditorResources.getImage( "schema" )); //$NON-NLS-1$
        }
        else if (dbObject instanceof DBSTable) {
            fType = TABLE_OBJTYPE;
            fName = dbObject.getName();
            fParentName = dbObject.getParentObject().getName();
            fParentObject = dbObject.getParentObject();
            fGrandParentName = fParentObject == null ? null : fParentObject.getParentObject() == null ? null : fParentObject.getParentObject().getName();
            fGrandGrandParentName = null;
/*
            if (dbObject instanceof ViewTable) {
                setImage( SQLEditorResources.getImage( "view" )); //$NON-NLS-1$
            } else {
                setImage( SQLEditorResources.getImage( "table" )); //$NON-NLS-1$
            }
*/
        }
        else if (dbObject instanceof DBSTableColumn) {
/*
            DataTypeProvider provider = SQLToolsFacade.getConfigurationByVendorIdentifier(
                    ModelUtil.getDatabaseVendorDefinitionId(((Column) dbObject).getTable().getSchema()))
                    .getSQLDataService().getDataTypeProvider();
            String datatypeStr = provider.getDataTypeString(((Column) dbObject).getDataType(), false);
*/
/*
            if (((DBSTableColumn) dbObject).getDataType().getDataKind() instanceof PredefinedDataType && datatypeStr != null)
            {
                datatypeStr = datatypeStr.toLowerCase();
            }
*/

            fType = TABLECOLUMN_OBJTYPE;
            fName = dbObject.getName() + " - " + ((DBSTableColumn) dbObject).getTypeName(); //$NON-NLS-1$;
            fParentName = ((DBSTableColumn) dbObject).getTable().getParentObject().getName()
                    + "." + ((DBSTableColumn) dbObject).getTable().getName();
            fParentObject = dbObject.getParentObject();
            fGrandParentName = fParentObject.getParentObject().getName();
            fGrandParentName = fParentObject == null ? null : fParentObject.getParentObject() == null ? null : fParentObject.getParentObject().getName();

/*
            if (((DBSTableColumn) dbObject).isPartOfPrimaryKey())
            {
                setImage(SQLEditorResources.getImage("column_pkey")); //$NON-NLS-1$
            }
            else if (((Column) dbObject).isPartOfForeignKey())
            {
                setImage(SQLEditorResources.getImage("column_fkey")); //$NON-NLS-1$
            }
            else if (((Column) dbObject).isNullable())
            {
                setImage(SQLEditorResources.getImage("column_null")); //$NON-NLS-1$
            }
            else
            {
                setImage(SQLEditorResources.getImage("column")); //$NON-NLS-1$
            }
*/
        }
/*
        else if (dbObject instanceof Function) {
        	fType = FUNCTION_OBJTYPE;
        	fName = ((Function) dbObject).getName();
        	fParentName = ((Function) dbObject).getSchema().getName();
        	fParentObject = ((Function) dbObject).getSchema();
        	fGrandParentName = ModelUtil.getDatabaseName((Schema) fParentObject);
        	fGrandGrandParentName = null;
        	setImage( SQLEditorResources.getImage( "function" )); //$NON-NLS-1$
        }
*/
        else if (dbObject instanceof DBSProcedure) {
        	fType = STORED_PROCEDURE_OBJTYPE;
        	fName = dbObject.getName();
        	fParentName = dbObject.getParentObject().getName();
        	fParentObject = dbObject.getParentObject();
        	fGrandParentName = fParentObject == null ? null : fParentObject.getParentObject() == null ? null : fParentObject.getParentObject().getName();
        	fGrandGrandParentName = null;
        	//setImage( SQLEditorResources.getImage( "procedure" )); //$NON-NLS-1$
        }
/*
        else if (dbObject instanceof Event) {
        	fType = EVENT_OBJTYPE;
        	fName = ((Event) dbObject).getName();
        	fParentName = ((Event) dbObject).getDatabase().getName();
        	fParentObject = ((Event) dbObject).getDatabase();
        	fGrandParentName = null;
        	fGrandGrandParentName = null;
        	setImage( SQLEditorResources.getImage( "event" )); //$NON-NLS-1$
        }
        else if (dbObject instanceof Trigger) {
        	fType = TRIGGER_OBJTYPE;
        	fName = ((Trigger) dbObject).getName();
            fParentName = ((Trigger) dbObject).getSubjectTable().getSchema().getName()
            + "." + ((Trigger) dbObject).getSubjectTable().getName();
            fParentObject = ((Trigger) dbObject).getSubjectTable();
            fGrandParentName = ((Table) fParentObject).getSchema().getName();
            fGrandGrandParentName = ModelUtil.getDatabaseName(((Table) fParentObject).getSchema());
            setImage( SQLEditorResources.getImage( "trigger" )); //$NON-NLS-1$
        }
        else if (dbObject instanceof Index) {
            fType = INDEX_OBJTYPE;
            fName = ((Index) dbObject).getName();
            fParentName = ((Index) dbObject).getTable().getSchema().getName()
            + "." + ((Index) dbObject).getTable().getName();
            fParentObject = ((Index) dbObject).getTable();
            fGrandParentName = ((Table) fParentObject).getSchema().getName();
            fGrandGrandParentName = ModelUtil.getDatabaseName(((Table) fParentObject).getSchema());
            setImage( SQLEditorResources.getImage( "index" )); //$NON-NLS-1$
        }
*/
        else if (dbObject instanceof DBSCatalog)
        {
            fType = CATALOG_OBJTYPE;
            fName = dbObject.getName();
            fParentObject = null;
            //setImage(SQLEditorResources.getImage( "database" ));
        }

    }

    /**
     * Gets the database model object that this proposal represents.
     *
     * @return the database model object
     */
    public DBSObject getDBObject() {
        return fDBObject;
    }

    /**
     * Gets the image to be used for this content assist proposal.
     *
     * @return the image for this model object type
     */
    public Image getImage() {
        return fImage;
    }

    /**
     * Gets the name of the database object.
     *
     * @return the name of the database object
     */
    public String getName() {
        return fName;
    }

    /**
     * Gets the alias of the database object.
     *
     * @return the alias of the database object, if none, equals to getParentName()
     */
    public String getParentAlias() {
    	if (fParentAlias != null)
    	{
    		return fParentAlias;
    	}
    	return fParentName;
    }

    /**
     * Gets the name of the parent of the database model object associated
     * with this proposal.
     *
     * @return the parent name
     */
    public String getParentName() {
        return fParentName;
    }

    /**
     * Gets the name of the grandparent of the database model object associated
     * with this proposal.
     *
     * @return the grand parent name
     */
    public String getGrandParentName() {
    	return fGrandParentName;
    }

    /**
     * Gets the name of the grandgrandparent of the database model object associated
     * with this proposal.
     *
     * @return the grand parent name
     */
    public String getGrandGrandParentName() {
    	return fGrandGrandParentName;
    }

    /**
     * Gets the parent of the database object.
     *
     * @return the parent of the database object
     */
    public DBSObject getParentObject() {
        return fParentObject;
    }

    /**
     * Gets the type of the proposal.
     *
     * @return type of proposal, which is one of
     *         <ol>
     *         <li>SCHEMA_OBJTYPE
     *         <li>TABLE_OBJTYPE
     *         <li>TABLECOLUMN_OBJTYPE <eol>
     */
    public int getType() {
        return fType;
    }

    /**
     * Sets the image to be used for this content assist proposal.
     *
     * @param image the <code>Image</code> to use for this proposal
     */
    public void setImage( Image image ) {
        fImage = image;
    }

    /**
     * Sets the parent name to the given name.
     *
     * @param parentName the parent name to set
     */
    public void setParentName( String parentName ) {
        fParentName = parentName;
    }

    /**
     * Sets the type for this content assist proposal.
     *
     * @param type the type to set
     */
    public void setType( int type ) {
        fType = type;
    }

    /**
     * Sets the name to the given name.
     *
     * @param name the name to set
     */
    public void setName( String name ) {
        fName = name;
    }

    /**
     * Gets a string describing this object.  The name attribute of this object
     * is returned.
     *
     * @return the string describing this object.
     */
    public String toString() {
        return fName;
    }
}