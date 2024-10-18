package org.jkiss.dbeaver.ext.cubrid.ui.editors;

import org.eclipse.jface.viewers.Viewer;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.ui.editors.sql.plan.simple.SQLPlanViewProviderSimple;

public class CubridSQLPlanViewProvider extends SQLPlanViewProviderSimple{
	private static final Log log = Log.getLog(CubridSQLPlanViewProvider.class);
	
	@Override
    public void visualizeQueryPlan(Viewer viewer, SQLQuery query, DBCPlan plan) {
		try {
			query.setText(plan.getPlanQueryString());
		} catch (DBException e) {
			log.debug("Cubrid: could not set query text", e);
		}
        fillPlan(query,plan);
        showPlan(viewer,query, plan);
    }
}
