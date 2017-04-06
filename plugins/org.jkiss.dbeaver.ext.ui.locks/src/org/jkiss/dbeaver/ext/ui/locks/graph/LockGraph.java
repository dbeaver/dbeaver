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

import org.jkiss.dbeaver.ext.ui.locks.manage.LockManagerViewer;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;


public class LockGraph {

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
