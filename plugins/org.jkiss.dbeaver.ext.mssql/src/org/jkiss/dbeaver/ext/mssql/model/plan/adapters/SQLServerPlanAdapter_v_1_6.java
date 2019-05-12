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

package org.jkiss.dbeaver.ext.mssql.model.plan.adapters;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.jkiss.dbeaver.ext.mssql.model.plan.SQLServerPlanAdapter;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.ShowPlanXML;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.utils.xml.XMLException;


public class SQLServerPlanAdapter_v_1_6 extends SQLServerPlanAdapter<ShowPlanXML>{



    @Override
    public List<DBCPlanNode> getNodes(String planString) throws XMLException {
        List<DBCPlanNode> nodes = Collections.emptyList();
        
        try {
            
            ShowPlanXML plan = parseXML(planString);
              
            System.out.println(plan);

            
        } catch (JAXBException e) {
           throw new XMLException("Unable to parse plan",e);
        }

        
        return nodes;
    }


    @Override
    protected Class<ShowPlanXML> getPlanClass() {
        return ShowPlanXML.class;
    }

}
