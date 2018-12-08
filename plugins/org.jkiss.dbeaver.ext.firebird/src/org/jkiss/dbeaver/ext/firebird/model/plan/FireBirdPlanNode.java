/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
