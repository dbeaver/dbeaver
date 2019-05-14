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

package org.jkiss.dbeaver.ext.mssql.model.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.RelOpType_sql2017;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;

public class SQLServerPlanNode extends AbstractExecutionPlanNode {

    private final RelOpType_sql2017 node;

    private String name;
    private final String type;
    private final SQLServerPlanNode parent;
    private final List<SQLServerPlanNode> nested = new ArrayList<>();
    

  /*
 
    
    @XmlAttribute(name = "Filtered")
    protected Boolean filtered;
    
    
    @XmlAttribute(name = "TableReferenceId")
    protected Integer tableReferenceId;
       
    @XmlAttribute(name = "CloneAccessScope")
    protected CloneAccessScopeType_sql2017 cloneAccessScope;
    
    @XmlAttribute(name = "Storage")
    protected StorageType_sql2017 storage;
    
 */

    
    public SQLServerPlanNode(String name, String type, RelOpType_sql2017 node,SQLServerPlanNode parent) {
        this.name = name;
        this.type = type;
        this.parent = parent;
        this.node = node;
    }

    @Override
    public String getNodeName() {
        return name;
    }

     @Override
    public String getNodeType() {
        return type;
    }

    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

     @Override
    public Collection<? extends DBCPlanNode> getNested() {
        return nested;
    }

    public RelOpType_sql2017 getNode() {
        return node;
    }
     
    public void addNested(SQLServerPlanNode node) {
        nested.add(node);
    }

    @Override
    public String toString() {
        return "SQLServerPlanNode [name=" + name + ", type=" + type + "]";
    }

    public void setName(String name) {
        this.name = name;
    }
    
    

}
