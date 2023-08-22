package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.LazyProperty;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObjectLazy;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

/**
 * Dm Table Index
 * 
 * @author caosw
 *
 */
public class DmTableIndex extends JDBCTableIndex<DmSchema, DmTablePhysical> implements DBSObjectLazy, DBPScriptObject {

	private Object tablespace;
	private boolean nonUnique;
	private List<DmTableIndexColumn> columns;
	private String indexDDL;

	public DmTableIndex(
        DmSchema schema,
        DmTablePhysical table,
        String indexName,
        ResultSet dbResult)
    {
        super(schema, table, indexName, null, true);
        String indexTypeName = JDBCUtils.safeGetString(dbResult, "INDEX_TYPE");
        this.nonUnique = !"Y".equals(JDBCUtils.safeGetString(dbResult, "UNIQUENESS"));
        if("BT".equals(indexTypeName)) {
        	indexTypeName="NORMAL";
        }
        if (DmConstants.INDEX_TYPE_NORMAL.getId().equals(indexTypeName)) {
            indexType = DmConstants.INDEX_TYPE_NORMAL;
        } else if (DmConstants.INDEX_TYPE_BITMAP.getId().equals(indexTypeName)) {
            indexType = DmConstants.INDEX_TYPE_BITMAP;
        } else if (DmConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL.getId().equals(indexTypeName)) {
            indexType = DmConstants.INDEX_TYPE_FUNCTION_BASED_NORMAL;
        } else if (DmConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP.getId().equals(indexTypeName)) {
            indexType = DmConstants.INDEX_TYPE_FUNCTION_BASED_BITMAP;
        } else if (DmConstants.INDEX_TYPE_DOMAIN.getId().equals(indexTypeName)) {
            indexType = DmConstants.INDEX_TYPE_DOMAIN;
        } else {
            indexType = DBSIndexType.OTHER;
        }
        this.tablespace = JDBCUtils.safeGetString(dbResult, "TABLESPACE_NAME");
    }

	public DmTableIndex(DmSchema schema, DmTablePhysical parent, String name, boolean unique, DBSIndexType indexType)
    {
        super(schema, parent, name, indexType, false);
        this.nonUnique = !unique;

    }

	@NotNull
	@Override
	public DmDataSource getDataSource() {
		return getTable().getDataSource();
	}

	@Override
	@Property(viewable = true, order = 5)
	public boolean isUnique() {
		return !nonUnique;
	}

	public void setUnique(boolean unique) {
		this.nonUnique = !unique;
	}

	@Override
	public Object getLazyReference(Object propertyId) {
		return tablespace;
	}

	@Property(viewable = true, order = 10)
	@LazyProperty(cacheValidator = DmTablespace.TablespaceReferenceValidator.class)
	public Object getTablespace(DBRProgressMonitor monitor) throws DBException {
		return DmTablespace.resolveTablespaceReference(monitor, this, null);
	}

	@Nullable
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public List<DmTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor) {
		return columns;
	}

	@Nullable
	@Association
	public DmTableIndexColumn getColumn(String columnName) {
		return DBUtils.findObject(columns, columnName);
	}

	void setColumns(List<DmTableIndexColumn> columns) {
		this.columns = columns;
	}

	public void addColumn(DmTableIndexColumn column) {
		if (columns == null) {
			columns = new ArrayList<>();
		}
		columns.add(column);
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), this);
	}

	@Override
	public String toString() {
		return getFullyQualifiedName(DBPEvaluationContext.UI);
	}

	@Override
	@Property(hidden = true, editable = true, updatable = true, order = -1)
	public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
		if (indexDDL == null && isPersisted()) {
			try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read index definition")) {
				indexDDL = JDBCUtils.queryString(session, "SELECT DBMS_METADATA.GET_DDL('INDEX', ?, ?) TXT FROM DUAL",
						getName(), getTable().getSchema().getName());
			} catch (SQLException e) {
				throw new DBException(e, getDataSource());
			}
		}
		return indexDDL;
	}
}
