package org.jkiss.dbeaver.ui.views.lock;

import java.util.ArrayList;
import java.util.List;

import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;


public class LockGraphNode  {
	
	private int level;

	private int span;
	
	private String title;
	
	private DBAServerLock<?> lock;
	
	public enum LevelPosition { LEFT, CENTER, RIGHT};	
	
	private LevelPosition levelPosition;

	public LevelPosition getLevelPosition() {
		return levelPosition;
	}

	public void setLevelPosition(LevelPosition levelPosition) {
		this.levelPosition = levelPosition;
	}

	public int getLevel() {
		return this.level;
	}  

	private List<LockGraphEdge> sourceEdges;

	private List<LockGraphEdge> targetEdges;
	
	public LockGraphNode(DBAServerLock<?> lock){
		
		this.lock = lock;
		this.level = 0;
		this.span = 0;
		this.title = lock.getTitle();
		this.sourceEdges = new ArrayList<LockGraphEdge>();
		this.targetEdges = new ArrayList<LockGraphEdge>();
		
		this.levelPosition = LevelPosition.CENTER;
		
	}

	public LockGraphNode(String title,int level,int span) {
		this.level = level;
		this.span = span;
		this.title = title;
		this.sourceEdges = new ArrayList<LockGraphEdge>();
		this.targetEdges = new ArrayList<LockGraphEdge>();
	}

	public void addSourceEdge(LockGraphEdge sourceEdge) {
		this.sourceEdges.add(sourceEdge);
	}

	public void addTargetEdge(LockGraphEdge targetEdge) {
		this.targetEdges.add(targetEdge);
	}

	public List<LockGraphEdge> getSourceEdges() {
		return this.sourceEdges;
	}

	public List<LockGraphEdge> getTargetEdges() {
		return this.targetEdges;
	}

	public void removeSourceEdge(LockGraphEdge sourceEdge) {
		this.sourceEdges.remove(sourceEdge);
	}

	public void removeTargetEdge(LockGraphEdge targetEdge) {
		this.targetEdges.remove(targetEdge);
	}

	public int getSpan() {
		return span;
	}

	public String getTitle() {
		return title;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public void setSpan(int span) {
		this.span = span;
	}

	public DBAServerLock<?> getLock() {
		return lock;
	}
	
	
}

