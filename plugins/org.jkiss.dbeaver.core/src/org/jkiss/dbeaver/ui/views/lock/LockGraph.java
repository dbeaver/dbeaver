package org.jkiss.dbeaver.ui.views.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;





public class LockGraph {

	protected int COUNT = 45;

	private List<LockGraphNode> nodes = new ArrayList<LockGraphNode>();
	
	private int maxWidth = 0;
	
	private LockGraphNode selection; 
	
	private LockManagerViewer lockManagerViewer;
	
	private final DBAServerLock<?> lockRoot;
	
	public DBAServerLock<?> getLockRoot() {
		return lockRoot;
	}

	public LockManagerViewer getLockManagerViewer() {
		return lockManagerViewer;
	}

	public void setLockManagerViewer(LockManagerViewer lockManagerViewer) {
		this.lockManagerViewer = lockManagerViewer;
	}

	public LockGraph(DBAServerLock<?> lockRoot) {
		this.selection = null;
		this.lockRoot = lockRoot;
	}

	public LockGraphNode getSelection() {
		return selection;
	}


	public void setSelection(LockGraphNode selection) {
		this.selection = selection;
	}


	protected void createNodes() {
		for (int i = 0; i < this.COUNT; i++) {
			LockGraphNode node = new LockGraphNode(String.valueOf(i),i,2); //FIXME
			getNodes().add(node);
		}
		for (int i = 0; i < this.COUNT; i++) {
			LockGraphEdge edge = new LockGraphEdge();			 
			edge.setSource((LockGraphNode)getNodes().get(ThreadLocalRandom.current().nextInt(0, this.COUNT)));
		    edge.setTarget(nodes.get(i));
		}
	}

	public List<LockGraphNode> getNodes() {
		return this.nodes;
	}


	public int getMaxWidth() {
		return maxWidth;
	}


	public void setMaxWidth(int maxWidth) {
		this.maxWidth = maxWidth;
	}
}
