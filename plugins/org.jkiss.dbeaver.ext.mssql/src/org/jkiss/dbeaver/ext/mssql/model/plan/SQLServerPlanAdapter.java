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

import java.io.StringReader;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.utils.xml.XMLException;
import org.xml.sax.InputSource;


public abstract class SQLServerPlanAdapter<PLANTYPE> {
    
    private static JAXBContext jaxbContext = null;
    
    Unmarshaller jaxbUnmarshaller = null;
    
    public abstract List<DBCPlanNode> getNodes(String planString) throws XMLException;
    
    protected abstract Class<PLANTYPE> getPlanClass();
    
    @SuppressWarnings("unchecked")
    protected PLANTYPE parseXML(String planString) throws JAXBException {
        
 
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(getPlanClass());
        }
        
        if (jaxbUnmarshaller == null) {
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        }
        
        return (PLANTYPE) jaxbUnmarshaller.unmarshal(new InputSource(new StringReader(planString)));
        
    }
}
