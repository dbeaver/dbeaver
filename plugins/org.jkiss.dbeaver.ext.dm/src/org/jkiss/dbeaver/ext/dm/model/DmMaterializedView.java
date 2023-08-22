package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.source.DmSourceObject;
import org.jkiss.dbeaver.ext.dm.model.utils.DmUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.DBSObjectState;

/**
 * DM DmMaterialized View Object
 * 
 * @author caosw
 *
 */
public class DmMaterializedView extends DmTableBase implements DmSourceObject, DBSObjectLazy<DmDataSource> {

	private Object container;
	private String query;
	private boolean rewriteEnabled;
	private String refreshMode;
	private String refreshMethod;
	private String lastRefreshType;
	private Date lastRefreshDate;
	private String staleness;
	private String mviewType;

	public DmMaterializedView(DmSchema schema, String name) {
		super(schema, name, false);
	}

	public DmMaterializedView(DmSchema schema, ResultSet dbResult) {
		super(schema, JDBCUtils.safeGetString(dbResult, "MVIEW_NAME"), true);
		this.query = JDBCUtils.safeGetString(dbResult, "QUERY");
		this.rewriteEnabled = JDBCUtils.safeGetBoolean(dbResult, "REWRITE_ENABLED", "Y");
		this.refreshMode = JDBCUtils.safeGetString(dbResult, "REFRESH_MODE");
		this.refreshMethod = JDBCUtils.safeGetString(dbResult, "REFRESH_METHOD");
		this.mviewType = JDBCUtils.safeGetString(dbResult, "MVIEW_TYPE");
		this.lastRefreshType = JDBCUtils.safeGetString(dbResult, "LAST_REFRESH_TYPE");
		this.staleness = JDBCUtils.safeGetString(dbResult, "STALENESS");
		this.lastRefreshDate = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REFRESH_DATE");
	}

	@Property(viewable = false, order = 15)
	public boolean isRewriteEnabled() {
		return rewriteEnabled;
	}

	@Property(viewable = false, order = 16)
	public String getRefreshMode() {
		return refreshMode;
	}

	@Property(viewable = true, order = 17)
	public String getRefreshMethod() {
		return refreshMethod;
	}

	@Property(viewable = true, order = 18)
	public String getLastRefreshType() {
		return lastRefreshType;
	}

	@Property(viewable = true, order = 19)
	public Date getLastRefreshDate() {
		return lastRefreshDate;
	}

	@Property(viewable = false, order = 20)
	public String getStaleness() {
		return staleness;
	}

	@Property(viewable = true, order = 21) 
	public String getMviewType() {
		return mviewType;
	}

	@Override
	public DmSourceType getSourceType() {
		return DmSourceType.MATERIALIZED_VIEW;
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) {
		return query;
	}

	public void setObjectDefinitionText(String source) {
		this.query = source;
	}

	@Override
	public DBEPersistAction[] getCompileActions(DBRProgressMonitor monitor) {
		return new DBEPersistAction[] {
				new DmObjectPersistAction(DmObjectType.MATERIALIZED_VIEW, "Compile materialized view",
						"ALTER MATERIALIZED VIEW " + getFullyQualifiedName(DBPEvaluationContext.DDL) + " COMPILE") };
	}

	@NotNull
	@Override
	public DBSObjectState getObjectState() {
		return valid ? DBSObjectState.NORMAL : DBSObjectState.INVALID;
	}

	@Override
	public boolean isView() {
		return true;
	}

	@Override
	public TableAdditionalInfo getAdditionalInfo() {
		return null;
	}

	@Override
	public String getTableTypeName() {
		return "MVIEW";
	}

	@Override
	public void refreshObjectState(@NotNull DBRProgressMonitor monitor) throws DBCException {
		this.valid = DmUtils.getObjectStatus(monitor, this, DmObjectType.MATERIALIZED_VIEW);
	}


	@Override
	public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return container;
	}

}
