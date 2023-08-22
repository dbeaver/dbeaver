package org.jkiss.dbeaver.ext.dm.model;

import java.sql.ResultSet;

import org.eclipse.core.commands.util.Tracing;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.dm.model.utils.DmConstants;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

/**
 * DM Table Constraint
 * 
 * @author caosw
 *
 */
public class DmTableConstraint extends DmTableConstraintBase {

	private static Log log = Log.getLog(DmTableConstraint.class);

	private String searchCondition;

	public DmTableConstraint(DmTableBase dmTable, String name, DBSEntityConstraintType constraintType,
			String searchCondition, DmObjectStatus status) {
		super(dmTable, name, constraintType, status, false);
		this.searchCondition = searchCondition;
	}

	public DmTableConstraint(DmTableBase dmTable, ResultSet rs) {
		super(dmTable, JDBCUtils.safeGetString(rs, "CONSTRAINT_NAME"),
				getConstraintType(JDBCUtils.safeGetString(rs, "CONSTRAINT_TYPE")),
				CommonUtils.notNull(
						CommonUtils.valueOf(DmObjectStatus.class, "Y".equals(JDBCUtils.safeGetStringTrimmed(rs, "STATUS"))?"ENABLED":JDBCUtils.safeGetStringTrimmed(rs, "STATUS")),
						DmObjectStatus.ENABLED),
				true);
		this.searchCondition = JDBCUtils.safeGetString(rs, "SEARCH_CONDITION");//获取检测约束 CHECK
	}

	@Property(viewable = true, editable = true,updatable = true, order = 4)
	public String getSearchCondition() {
		return searchCondition;
	}

	public void setSearchCondition(String searchCondition) { //设置CHeck 检测约束
		this.searchCondition = searchCondition;
	}

	@NotNull
	@Override
	public String getFullyQualifiedName(DBPEvaluationContext context) {
		return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
	}

	public static DBSEntityConstraintType getConstraintType(String code) {
		switch (code) {
		case "C":
			return DBSEntityConstraintType.CHECK;
		case "P":
			return DBSEntityConstraintType.PRIMARY_KEY;
		case "U":
			return DBSEntityConstraintType.UNIQUE_KEY;
		case "R":
			return DBSEntityConstraintType.FOREIGN_KEY;
		case "V":
			return DmConstants.CONSTRAINT_WITH_CHECK_OPTION;
		case "O":
			return DmConstants.CONSTRAINT_WITH_READ_ONLY;
		case "H":
			return DmConstants.CONSTRAINT_HASH_EXPRESSION;
		case "F":
			return DmConstants.CONSTRAINT_REF_COLUMN;
		case "S":
			return DmConstants.CONSTRAINT_SUPPLEMENTAL_LOGGING;
		default:
			log.debug("Unsupported Oracle constraint type: " + code);
			return DBSEntityConstraintType.CHECK;
		}
	}
}
