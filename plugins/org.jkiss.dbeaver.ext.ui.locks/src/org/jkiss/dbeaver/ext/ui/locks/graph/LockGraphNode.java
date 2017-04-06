/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2017 Andrew Khitrin (ahitrin@gmail.com) 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.ext.ui.locks.graph;

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

