/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.team.git.ui.utils;


import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;

public class SelectionUtil {
    
    private static DBNDataSource searchDS(DBNNode node) {
        if (node == null) return null;
        while (!(node instanceof DBNDataSource)) {
            if (node.getParentNode() == null) return null;
            node = node.getParentNode();
        }
        return (DBNDataSource) node;
    }
    
    public static IProject extractProject(ISelection selection) {
        
        StructuredSelection structSelection = new StructuredSelection(selection);
        
        if (selection != null && !structSelection.isEmpty() && structSelection.size() == 1) {
            Object e = structSelection.getFirstElement();
            if (e != null && e instanceof StructuredSelection) {
                StructuredSelection ts = (StructuredSelection) e;
                Object o =  ts.getFirstElement();
                if (ts.isEmpty()) return null;
                if (o instanceof DBNNode) {
                    DBNDataSource ds = searchDS((DBNNode) o);
                    return ds == null ? null : ds.getOwnerProject();
                }else {
                    return null;
                }
            }           
            return null;
        } else {
            return null;
        }
        
    }

}
