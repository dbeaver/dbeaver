package org.jkiss.dbeaver.ext.exasol.model;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolSourceObject;
import org.jkiss.dbeaver.ext.exasol.editors.ExasolStatefulObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

public class ExasolView  extends ExasolTableBase implements ExasolSourceObject {

	
	private String owner;
	private String name;
	private String description;
	private ExasolSchema schema;

	private String text;

	public ExasolView(ExasolSchema schema, String name, boolean persisted) {
		super(schema, name, persisted);
	}

	public ExasolView(DBRProgressMonitor monitor, ExasolSchema schema, ResultSet dbResult) {
		super(monitor, schema, dbResult);
		this.text = JDBCUtils.safeGetString(dbResult, "VIEW_TEXT");
		this.schema = schema;
		
	}


	
	@Override
	public DBSObjectState getObjectState() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public String getDescription() {
		return this.description;
	}
	
    // -----------------
    // Properties
    // -----------------

	@NotNull
    @Property(viewable = true, order = 100)
    public String getOwner() {
		return owner;
	}


	@NotNull
    @Property(viewable = true, order = 100)
	public String getText() {
		return text;
	}


    

	@Override
	public boolean isView() {
		return true;
	}


    // -----------------
    // Business Contract
    // -----------------

    @Override
    public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException
    {
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
    	super.refreshObject(monitor);
        return this;
    }
	
	
    // -----------------
    // Associations (Imposed from DBSTable). In Exasol, Most of objects "derived"
    // from Tables don't have those..
    // -----------------
	@Override
	public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
		return Collections.emptyList();
	}


	@Override
	public Collection<ExasolTableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException {
		return Collections.emptyList();
	}

	@Override
	public JDBCStructCache<ExasolSchema, ? extends DBSEntity, ? extends DBSEntityAttribute> getCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
		return SQLUtils.formatSQL(getDataSource(), this.text);
		
	}
	



}
