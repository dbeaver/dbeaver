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
package org.jkiss.dbeaver.ext.ui.locks.manage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jkiss.dbeaver.ext.ui.locks.graph.LockGraph;
import org.jkiss.dbeaver.ext.ui.locks.graph.LockGraphEdge;
import org.jkiss.dbeaver.ext.ui.locks.graph.LockGraphNode;
import org.jkiss.dbeaver.model.admin.locks.DBAServerLock;

public abstract class LockGraphManager<LOCK_TYPE extends DBAServerLock<?>, ID_TYPE> {

	private Map<ID_TYPE,LockGraphNode> nodes = new HashMap<ID_TYPE,LockGraphNode>();
	
    private Map<ID_TYPE,LockGraph> graphIndex = new HashMap<ID_TYPE,LockGraph>();	
    

	public LockGraph getGraph(DBAServerLock<?> curLock) {
		
		LockGraphNode selection = nodes.get(curLock.getId());
		
		LockGraph graph = graphIndex.get(curLock.getId());
		
		if (graph != null && selection != null) {
			graph.setSelection(selection);
		}
		
		return graph;
		
	}
	
	@SuppressWarnings("unchecked")
	private LockGraph createGraph(LOCK_TYPE root) {
	
		
		LockGraph graph = new LockGraph(root);
		
		int maxWidth = 1;
		
        int level = 1;
        
        LockGraphNode nodeRoot = nodes.get(root.getId());
        
        nodeRoot.setLevel(0);
        
        nodeRoot.setSpan(1);
        
        graph.getNodes().add(nodeRoot);
        
        graphIndex.put((ID_TYPE) root.getId(), graph);
    	
        List<LOCK_TYPE> current = new ArrayList<LOCK_TYPE>();
        
        Set<DBAServerLock<ID_TYPE>> touched = new HashSet<>(); //Prevent Cycle
        
        current.add(root);
        
        touched.add((DBAServerLock<ID_TYPE>) root);
        
        Map<ID_TYPE,DBAServerLock<ID_TYPE>> childs = new HashMap<ID_TYPE,DBAServerLock<ID_TYPE>>();
        
        while(current.size() > 0) {
        	
            if (maxWidth < current.size()) {
				
				maxWidth = current.size();
				
			}
            
            for(int index = 0; index < current.size(); index++) {
            	
            	DBAServerLock<ID_TYPE> l  = (DBAServerLock<ID_TYPE>) current.get(index);
            	
            	LockGraphNode node = nodes.get(l.getId());
            	
            	if (index == 0) {
            		node.setLevelPosition(LockGraphNode.LevelPosition.LEFT);
            	} else if (index == current.size() - 1) {
            		node.setLevelPosition(LockGraphNode.LevelPosition.RIGHT);
            	} else {
            		node.setLevelPosition(LockGraphNode.LevelPosition.CENTER);
            	}
            	
            	node.setSpan(current.size());
            	
            	
            	for(DBAServerLock<ID_TYPE> c : l.waitThis()) {
            		
            		if (touched.contains(c)) continue;
            		
            		touched.add(c);
            		
					childs.put(c.getId(), c);
					
					graphIndex.put(c.getId(), graph);
					
					LockGraphNode nodeChild = nodes.get(c.getId());
					
					graph.getNodes().add(nodeChild);
					
					nodeChild.setLevel(level);
					
					LockGraphEdge edge = new LockGraphEdge();	
	    			edge.setSource(node);
	    			edge.setTarget(nodeChild);

				}
            	
            	
            }

            level++;
			
    		current = new ArrayList<LOCK_TYPE>((Collection<? extends LOCK_TYPE>) childs.values());	
    		
    		childs.clear();

        	
        }
        
        graph.setMaxWidth(maxWidth);
        
		return graph;
		
	}
	
	

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void buildGraphs(Map<ID_TYPE,LOCK_TYPE> locks) {
		
		Set<LOCK_TYPE> roots = new HashSet<LOCK_TYPE>();
		
		this.nodes.clear();
		
		this.graphIndex.clear();
		
		for(LOCK_TYPE l: locks.values()) {
			
			if (locks.containsKey(l.getHoldID()) && (!l.getHoldID().equals(l.getId()))) {
				
				LOCK_TYPE holder = locks.get(l.getHoldID());
				l.setHoldBy(holder);
				holder.waitThis().add((DBAServerLock) l);
				
			} else {
				
				roots.add(l);
				
			}
			
			nodes.put((ID_TYPE) l.getId(), new LockGraphNode(l));
		}
		
		for(LOCK_TYPE root : roots) {
		
			createGraph(root);
			
		}
		
	}
	 
}
