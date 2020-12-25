package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDInsertReplaceMethod;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.Optional;

public class OracleInsertReplaceMethodIgnore implements DBDInsertReplaceMethod {
    private static final Log log = Log.getLog(OracleInsertReplaceMethodIgnore.class);

    @NotNull
    @Override
    public String getOpeningClause(DBSTable table, DBRProgressMonitor monitor) {
        if (table != null) {
            try {
                Collection<? extends DBSTableConstraint> constraints = table.getConstraints(monitor);
                if (!CommonUtils.isEmpty(constraints)) {
                    Optional<? extends DBSTableConstraint> tableConstraint = constraints
                            .stream().filter(key -> key.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY).findFirst();
                    if (tableConstraint.isPresent()) {
                        DBSTableConstraint constraint = tableConstraint.get();
                        return "INSERT /*+ IGNORE_ROW_ON_DUPKEY_INDEX(" + table.getName() + ", " + constraint.getName() + ") */ INTO";
                    }
                }
            } catch (DBException e) {
                log.debug("Can't read table constraints list");
            }
        }
        return "INSERT INTO";
    }

    @Override
    public String getTrailingClause(DBSTable table, DBRProgressMonitor monitor, DBSAttributeBase[] attributes) {
        return null;
    }
}
