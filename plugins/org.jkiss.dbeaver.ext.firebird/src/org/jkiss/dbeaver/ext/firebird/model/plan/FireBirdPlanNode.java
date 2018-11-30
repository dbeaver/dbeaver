package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

public class FireBirdPlanNode implements DBCPlanNode {

	String plan;
	FireBirdPlanNode parent;
	private List<FireBirdPlanNode> nested;
	
	public FireBirdPlanNode(String plan) {
		this.plan = plan;
		this.nested = new ArrayList<>();
	}
	
	@Override
	public DBCPlanNode getParent() {
		return parent;
	}

	@Override
	public Collection<FireBirdPlanNode> getNested() {
		return nested;
	}
	
	@Override
    public String toString()
    {
        return plan;
    }
}
