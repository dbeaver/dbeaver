package org.jkiss.dbeaver.ext.firebird.model.plan;

import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

public class FireBirdPlanNode implements DBCPlanNode {

	String plan;
	FireBirdPlanNode parent;
	private List<FireBirdPlanNode> nested;
	
	public FireBirdPlanNode(String plan) {
		this.plan = plan;
		if (parent == null) {
			parent = this;
		}
	}
	
	@Override
	public DBCPlanNode getParent() {
		return parent;
	}

	@Override
	public Collection<? extends DBCPlanNode> getNested() {
		return null;
	}
	
	@Override
    public String toString()
    {
        return plan;
    }
}
