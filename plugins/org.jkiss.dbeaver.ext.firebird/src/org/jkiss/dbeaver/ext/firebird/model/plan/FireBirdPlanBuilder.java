package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.ArrayList;
import java.util.List;
import org.jkiss.dbeaver.model.exec.DBCException;

public class FireBirdPlanBuilder {
	
	private String plan;
	
	public FireBirdPlanBuilder(String plan) {
		super();
		this.plan = plan;
	}

	public List<FireBirdPlanNode> Build() throws DBCException {
		List<FireBirdPlanNode> rootNodes = new ArrayList<>();
		String [] plans = plan.split("\\n");
		for (String plan: plans) {
			FireBirdPlanParser pm = new FireBirdPlanParser(plan);
			FireBirdPlanNode node = null;
			try {
				node = pm.parse();
			} catch (FireBirdPlanException e) {
				throw new DBCException(e.getMessage());
			}
			rootNodes.add(node);
		}
		return rootNodes;
	}
	
}
