package org.jkiss.dbeaver.ext.dm.model.session;

import java.sql.ResultSet;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPObjectWithDescription;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class DmServerExecutePlan implements DBPObjectWithDescription {


    private String plan;

    public DmServerExecutePlan(ResultSet dbResult) {
        this.plan = JDBCUtils.safeGetString(dbResult, "PLAN_TABLE_OUTPUT");
    }

    @Property(viewable = true, order = 1)
    public String getPlan() {
        return plan;
    }

    @Nullable
    @Override
    public String getDescription() {
        return plan;
    }
}
