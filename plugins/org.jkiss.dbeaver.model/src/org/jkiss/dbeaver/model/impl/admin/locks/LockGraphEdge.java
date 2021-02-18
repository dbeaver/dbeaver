/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.admin.locks;

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
