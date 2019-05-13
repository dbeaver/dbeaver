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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.BaseStmtInfoType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.CloneAccessScopeType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.IndexKindType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.ObjectType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.QueryPlanType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.RelOpBaseType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.RelOpType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.RowsetType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.ShowPlanXML;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.ShowPlanXML.BatchSequence_sql2017.Batch_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.StmtBlockType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.StmtSimpleType_sql2017;
import org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017.StorageType_sql2017;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;

import org.xml.sax.InputSource;

public class SQLServerPlanParser {

    public static final String rootNodeXPath = "/*[local-name() = 'ShowPlanXML']";
    public static final String VERSION_ATTR = "Version";

    private static final Log log = Log.getLog(SQLServerPlanParser.class);

    private JAXBContext jaxbContext = null;
    private Unmarshaller jaxbUnmarshaller = null;

    public static SQLServerPlanParser instance = new SQLServerPlanParser();

    private SQLServerPlanParser() {
    }

    public static SQLServerPlanParser getInstance() {
        return instance;
    }

    private ShowPlanXML parseXML(String planString) throws JAXBException {

        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(ShowPlanXML.class);
        }

        if (jaxbUnmarshaller == null) {
            jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        }

        return (ShowPlanXML) jaxbUnmarshaller.unmarshal(new InputSource(new StringReader(planString)));
    }

    private QueryPlanType_sql2017 findQueryPlan(ShowPlanXML plan, String query) {

        for (Batch_sql2017 batch : plan.getBatchSequence().getBatch()) {
            for (StmtBlockType_sql2017 stmt : batch.getStatements()) {
                for (BaseStmtInfoType_sql2017 s : stmt.getStmtSimpleOrStmtCondOrStmtCursor()) {
                    if (s instanceof StmtSimpleType_sql2017) {
                        if (((StmtSimpleType_sql2017) s).getStatementText().equals(query.trim())) {
                            return ((StmtSimpleType_sql2017) s).getQueryPlan();
                        }
                    }
                }
            }

        }

        return null;

    }

    public List<Method> getAccessibleMethods(Class clazz) {
        List<Method> result = new ArrayList<Method>();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers)) {
                    if (RelOpBaseType_sql2017.class.isAssignableFrom(method.getReturnType())) {
                        result.add(method);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return result;
    }

    private RowsetType_sql2017 findRowset(Object obj)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> clazz = obj.getClass();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                int modifiers = method.getModifiers();
                if (Modifier.isPublic(modifiers)) {
                    if (RowsetType_sql2017.class.isAssignableFrom(method.getReturnType())) {
                        RowsetType_sql2017 res = (RowsetType_sql2017) method.invoke(obj);
                        if (res != null) {
                            return res;
                        }
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        return null;
    }

    private List<RelOpType_sql2017> getRelOpChild(Object object) {

        List<RelOpType_sql2017> child = new ArrayList<RelOpType_sql2017>();

        try {
            Method method = object.getClass().getMethod("getRelOp");
            if (RelOpType_sql2017.class.isAssignableFrom(method.getReturnType())) {
                child.add((RelOpType_sql2017) method.invoke(object));
            } else if (List.class.isAssignableFrom(method.getReturnType())) {
                child.addAll((List<RelOpType_sql2017>) method.invoke(object));
            }
        } catch (NoSuchMethodException ne) {
            log.debug("Leaf node " + object.getClass());
        } catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {

            log.debug("Ignored in getRelOp in " + object.getClass(), e);
        }

        return child;
    }

    private String parseObject(ObjectType_sql2017 o,SQLServerPlanNode planNode) {
        
        
        StringBuilder sb = new StringBuilder();
        
        if (o.getIndex() != null) {
            
            if (o.getIndex() != null) {
                sb.append(o.getIndex());
            }
            
            if (o.getIndexKind() != null) {
                sb.append(" [").append(o.getIndexKind()).append("]");
            }
            
            sb.append(" ");
            
            
        } else if (o.getTable() != null) {
            
            if (o.getDatabase() != null) {
                sb.append(o.getDatabase()).append(".");
            }
            
            if (o.getSchema() != null) {
                sb.append(o.getSchema()).append(".");
            }
            
            if (o.getTable() != null) {
                sb.append(o.getTable());
            }
            
            if (o.getAlias() != null) {
                sb.append(" ").append(o.getAlias());
            }
            
            sb.append(" ");
        
        } else {
           return "";
            
        }
        
        return sb.toString();
    }

    private void setObjectName(Object obj,SQLServerPlanNode planNode) {
        
        final StringBuilder sb = new StringBuilder();

        try {

            RowsetType_sql2017 rowset = findRowset(obj);

            if (rowset == null) {
                return ;
            }

             rowset.getObject().stream().forEach(o -> {
                sb.append(parseObject(o,planNode));
                if (sb.length() > 0) {
                    sb.append(" ");
                }
            });

        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            log.debug(obj.getClass().getName() + " has no name");
            return ;
        }

        planNode.setName(sb.toString()); 

    }

    private void addChilds(SQLServerPlanNode nodeParent)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
       
        List<RelOpType_sql2017> childs = getChilds(nodeParent.getNode());
        
        for (RelOpType_sql2017 child : childs) {
            
            if (child != null) {
                
                SQLServerPlanNode node = new SQLServerPlanNode("", child.getLogicalOp().value(),
                        child, nodeParent);
                
                setObjectName(child,node);
            
                nodeParent.addNested(node);
                addChilds(node);
                
            }
            

        }

    }

    private List<RelOpType_sql2017> getChilds(RelOpType_sql2017 node)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        List<RelOpType_sql2017> childs = new ArrayList<>();

        List<Method> allChildMethods = getAccessibleMethods(node.getClass());

        for (Method method : allChildMethods) {

            Object result = method.invoke(node);

            if (result != null) {

                childs.addAll(getRelOpChild(result));

            }

        }

        return childs;

    }

    public List<DBCPlanNode> parse(String planString, String sqlString) throws DBCException {

        List<DBCPlanNode> nodes = new ArrayList<>();

        try {

            ShowPlanXML plan = parseXML(planString);

            QueryPlanType_sql2017 queryPlan = findQueryPlan(plan, sqlString);

            if (queryPlan == null) {
                throw new DBCException("Unable to find plan");
            }

            RelOpType_sql2017 relOpRoot = queryPlan.getRelOp();

            SQLServerPlanNode root = new SQLServerPlanNode("",
                    relOpRoot.getLogicalOp().value(), relOpRoot, null);

            setObjectName(relOpRoot,root);
            
            addChilds(root);

            nodes.add(root);

            return nodes;

        } catch (JAXBException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {

            throw new DBCException("Error parsing plan", e);
        }

    }
}
