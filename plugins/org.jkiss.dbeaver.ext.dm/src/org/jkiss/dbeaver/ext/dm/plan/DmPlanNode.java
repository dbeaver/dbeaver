package org.jkiss.dbeaver.ext.dm.plan;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

/**
 * DM execution plan node
 * 
 * @author caosw
 *
 */
public class DmPlanNode extends AbstractExecutionPlanNode implements DBCPlanCostNode {
	protected String name;
	protected String additional_info;
	protected Long cost;
	protected Long result;
	protected Long rowdatalength;
	protected String describe;

	protected DmPlanNode parent;
	protected List<DmPlanNode> nested;

	public DmPlanNode(List<DmPlanNode> nodes) {
		if (!nodes.isEmpty()) {
			this.result = nodes.get(0).result;
		}
		this.nested = nodes;
	}

	public DmPlanNode(DmPlanNode parent, ResultSet dbResult) {
		this.parent = parent;
		this.name = JDBCUtils.safeGetString(dbResult, "名称");
		this.additional_info = JDBCUtils.safeGetString(dbResult, "附加信息");
		this.cost = JDBCUtils.safeGetLong(dbResult, "代价");
		this.result = JDBCUtils.safeGetLong(dbResult, "结果集");
		this.rowdatalength = JDBCUtils.safeGetLong(dbResult, "行数据处理长度");
		this.describe = JDBCUtils.safeGetString(dbResult, "描述");
	}

	public DmPlanNode(DmPlanNode parent, Map<String, String> props) {
		this.parent = parent;
		this.name = props.get("名称");
		this.additional_info = props.get("附加信息");
		this.cost = props.containsKey("代价") ? CommonUtils.toLong(props.get("代价")) : null;
		this.result = props.containsKey("结果集") ? CommonUtils.toLong(props.get("结果集")) : null;
		this.rowdatalength = props.containsKey("行数据处理长度") ? CommonUtils.toLong(props.get("行数据处理长度")) : null;
		this.describe = props.get("描述");
	}

	protected DmPlanNode(DmPlanNode parent, DmPlanNode source) {
		this.name = source.name;
		this.additional_info = source.additional_info;
		this.cost = source.cost;
		this.result = source.result;
		this.rowdatalength = source.rowdatalength;
		this.describe = source.describe;
		this.parent = parent;
		if (source.nested != null) {
			this.nested = new ArrayList<DmPlanNode>(source.nested.size());
			for (DmPlanNode srcNode : source.nested) {
				this.nested.add(srcNode.copyNode(this));
			}
		}
	}

	@Override
	public DmPlanNode getParent() {
		return parent;
	}

	void setParent(DmPlanNode node) {
		if (this.parent != null && this.parent.nested != null) {
			this.parent.nested.remove(this);
		}
		this.parent = node;
		if (this.parent != null) {
			this.parent.addChild(this);
		}
	}

	private void addChild(DmPlanNode node) {
		if (this.nested == null) {
			this.nested = new ArrayList<DmPlanNode>();
		}
		this.nested.add(node);
	}

	@Override
	public Collection<DmPlanNode> getNested() {
		return nested;
	}

	@Override
	public String getNodeName() {
		return name;
	}

	@Override
	public String getNodeType() {
		return additional_info;
	}

	@Override
	public String getNodeDescription() {
		return describe;
	}

	@Property(order = 3, viewable = true)
	public long getCost() {
		return cost;
	}

	@Property(order = 4, viewable = true)
	public long getResult() {
		return result;
	}

	@Property(order = 5, viewable = true)
	public long getRowdatalength() {
		return rowdatalength;
	}

	DmPlanNode copyNode(DmPlanNode parent) {
		return new DmPlanNode(parent, this);
	}

	@Override
	public Number getNodeCost() {
		return cost;
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
		return result;
	}
}
