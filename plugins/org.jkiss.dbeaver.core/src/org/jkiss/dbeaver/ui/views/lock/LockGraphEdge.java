package org.jkiss.dbeaver.ui.views.lock;

public class LockGraphEdge {

	private LockGraphNode source;

	private LockGraphNode target;

	public LockGraphNode getSource() {
		return this.source;
	}

	public LockGraphNode getTarget() {
		return this.target;
	}

	public void setSource(LockGraphNode newSource) {
		if (this.source != null) {
			this.source.removeSourceEdge(this);
		}
		this.source = newSource;
		if (this.source != null) {
			this.source.addSourceEdge(this);
		}
	}

	public void setTarget(LockGraphNode newTarget) {
		if (this.target != null) {
			this.target.removeTargetEdge(this);
		}
		this.target = newTarget;
		if (this.target != null) {
			this.target.addTargetEdge(this);
		}
	}
}
