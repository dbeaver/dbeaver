/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.oracle.model.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.*;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.utils.CommonUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;

/**
 * Oracle execution plan node
 */
public class OracleQueryPlanner  extends AbstractExecutionPlanSerializer implements DBCQueryPlanner/*, DBCSavedQueryPlanner*/ {

    private final static String FORMAT_VERSION = "1";

    private final OracleDataSource dataSource;    


    public OracleQueryPlanner(OracleDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public OracleDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query) throws DBException {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(dataSource, (JDBCSession) session, query);
        plan.explain();
        return plan;
    }

    /*
    @Override
    public DBCPlan readSavedQueryExecutionPlan(DBCSession session, Object savedQueryId) throws DBException {
        OraclePlanAnalyser plan = new OraclePlanAnalyser(dataSource, (JDBCSession) session, savedQueryId);
        plan.readHistoric();
        return plan;
    }
     */

    @Override
    public DBCPlanStyle getPlanStyle() {
        return DBCPlanStyle.PLAN;
    }

    private JsonObject createAttr(String key,String value) {
        JsonObject attr = new JsonObject();
        attr.add(key,new JsonPrimitive(CommonUtils.notEmpty(value)));
        return attr; 
    }

    private JsonObject createAttr(String key,long value) {
        JsonObject attr = new JsonObject();
        attr.add(key,new JsonPrimitive(value));
        return attr; 
    }

    private JsonObject createAttr(String key,Timestamp value) {
        JsonObject attr = new JsonObject();
        attr.add(key,new JsonPrimitive(value.toInstant().toEpochMilli()));
        return attr; 
    }

    @Override
    public void serialize(Writer planData, DBCPlan plan) throws IOException {
        serializeJson(planData, plan,dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return FORMAT_VERSION;
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {

                JsonArray attributes = new JsonArray();
                if (node instanceof OraclePlanNode) {					
                    OraclePlanNode oraNode = (OraclePlanNode) node;
                    attributes.add(createAttr("statement_id", oraNode.getStatementId()));
                    attributes.add(createAttr("plan_id", oraNode.getPlanId()));
                    attributes.add(createAttr("timestamp", oraNode.getTimestamp()));
                    attributes.add(createAttr("remarks", oraNode.getRemarks()));
                    attributes.add(createAttr("operation", oraNode.getOperation()));
                    attributes.add(createAttr("options", oraNode.getOptions()));
                    attributes.add(createAttr("object_node", oraNode.getObjectNode()));
                    attributes.add(createAttr("object_owner", oraNode.getObjectOwner()));
                    attributes.add(createAttr("object_name", oraNode.getObjectName()));
                    attributes.add(createAttr("object_alias", oraNode.getObjectAlias()));
                    attributes.add(createAttr("object_instance", oraNode.getObjectInstance()));
                    attributes.add(createAttr("object_type", oraNode.getObjectType()));
                    attributes.add(createAttr("optimizer", oraNode.getOptimizer()));
                    attributes.add(createAttr("search_columns", oraNode.getSearchColumns()));
                    attributes.add(createAttr("id", oraNode.getId()));
                    attributes.add(createAttr("depth", oraNode.getDepth()));
                    attributes.add(createAttr("position", oraNode.getPosition()));
                    attributes.add(createAttr("cost", oraNode.getCost()));
                    attributes.add(createAttr("cardinality", oraNode.getCardinality()));
                    attributes.add(createAttr("bytes", oraNode.getBytes()));
                    attributes.add(createAttr("other_tag", oraNode.getOtherTag()));
                    attributes.add(createAttr("partition_start", oraNode.getPartitionStart()));
                    attributes.add(createAttr("partition_stop", oraNode.getPartitionStop()));
                    attributes.add(createAttr("partition_id", oraNode.getPartitionId()));
                    attributes.add(createAttr("other", oraNode.getOther()));
                    attributes.add(createAttr("distribution", oraNode.getDistribution()));
                    attributes.add(createAttr("cpu_cost", oraNode.getCpuCost()));
                    attributes.add(createAttr("io_cost", oraNode.getIoCost()));
                    attributes.add(createAttr("temp_space", oraNode.getTempSpace()));
                    attributes.add(createAttr("access_predicates", oraNode.getAccessPredicates()));
                    attributes.add(createAttr("filter_predicates", oraNode.getFilterPredicates()));
                    attributes.add(createAttr("projection", oraNode.getProjection()));
                    attributes.add(createAttr("time", oraNode.getTime()));
                    attributes.add(createAttr("qblock_name", oraNode.getQblockName()));
                    attributes.add(createAttr("other_xml", oraNode.getOtherXml()));
                    attributes.add(createAttr("parent_id", oraNode.getParentId()));				   
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);

            }
        });
    }


    @Override
    public DBCPlan deserialize(Reader planData) throws IOException,InvocationTargetException {
        OraclePlanLoader plan = new OraclePlanLoader();
        plan.deserialize(dataSource, planData);
        return plan;
    }

}