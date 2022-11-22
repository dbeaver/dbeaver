package org.jkiss.dbeaver.ext.tidb.model.plan;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanNode;
import org.jkiss.dbeaver.ext.mysql.model.plan.MySQLPlanNodePlain;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;

public class TiDBPlanNodePlain extends MySQLPlanNode {
	protected String id;
	// Before 4.0 estRows named "count"
    protected String estRows;
    protected String task;
    protected String accessObject;
    protected String operatorInfo;
    
    protected TiDBPlanNodePlain parent;
    protected List<TiDBPlanNodePlain> nested;

    public TiDBPlanNodePlain(List<TiDBPlanNodePlain> nodes) {
        // Root node
    	id = "<plan>";
        if (!nodes.isEmpty()) {
            this.estRows = nodes.get(0).estRows;
        }
        this.nested = nodes;
    }
    
    public TiDBPlanNodePlain(TiDBPlanNodePlain parent, ResultSet dbResult) {
        this.parent = parent;
        this.id = JDBCUtils.safeGetString(dbResult, "id");
        this.estRows = JDBCUtils.safeGetString(dbResult, "estRows");
        this.task = JDBCUtils.safeGetString(dbResult, "task");
        this.accessObject = JDBCUtils.safeGetString(dbResult, "access object");
        this.operatorInfo = JDBCUtils.safeGetString(dbResult, "operator info");
    }
    
    @Override
    public TiDBPlanNodePlain getParent() {
        return parent;
    }

	@Override
	public Number getNodeCost() {
		return null;
	}

	@Override
	public Number getNodePercent() {
		return null;
	}

	@Override
	public Number getNodeDuration() {
		return null;
	}

	@Override
	public Number getNodeRowCount() {
		return Double.parseDouble(this.estRows);
	}

	@Override
	public String getNodeName() {
		return this.accessObject;
	}

	@Override
	public String getNodeType() {
		return this.id.trim().replaceAll("└", "").replaceAll("─", "");
	}

	@Override
	public Collection<? extends DBCPlanNode> getNested() {
		return this.nested;
	}
    
	@Property(order = 0, viewable = true)
    public String getId() {
        return id;
    }

    @Property(order = 1, name="estRows", viewable = true)
    public String getESTRows() {
        return estRows;
    }

    @Property(order = 2, name="task", viewable = true)
    public String getTask() {
        return task;
    }

    @Property(order = 3, name="access object", viewable = true)
    public String getAccessObject() {
        return accessObject;
    }

    @Property(order = 4, name="operator info", viewable = true)
    public String getOperatorInfo() {
        return operatorInfo;
    }
    
    void setParent(TiDBPlanNodePlain node) {
        if (this.parent != null && this.parent.nested != null) {
            this.parent.nested.remove(this);
        }
        this.parent = node;
        if (this.parent != null) {
            this.parent.addChild(this);
        }
    }

    private void addChild(TiDBPlanNodePlain node) {
        if (this.nested == null) {
            this.nested = new ArrayList<>();
        }
        this.nested.add(node);

    }
}
