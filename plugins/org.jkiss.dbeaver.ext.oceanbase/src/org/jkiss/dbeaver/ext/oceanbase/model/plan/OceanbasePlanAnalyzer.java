package org.jkiss.dbeaver.ext.oceanbase.model.plan;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oceanbase.mysql.model.OceanbaseMySQLDataSource;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.plan.DBCPlan;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanStyle;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlanner;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerConfiguration;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerSerialInfo;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanSerializer;
import org.jkiss.dbeaver.model.impl.plan.ExecutionPlanDeserializer;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class OceanbasePlanAnalyzer extends AbstractExecutionPlanSerializer implements DBCQueryPlanner {
    private OceanbaseMySQLDataSource dataSource;

    public OceanbasePlanAnalyzer(OceanbaseMySQLDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public OceanbasePlanJSON explain(JDBCSession session, String query) throws DBCException {
        final SQLDialect dialect = SQLUtils.getDialectFromObject(session.getDataSource());
        final String plainQuery = SQLUtils.stripComments(dialect, query).toUpperCase();
        final String firstKeyword = SQLUtils.getFirstKeyword(dialect, plainQuery);
        if (!"SELECT".equalsIgnoreCase(firstKeyword) && !"WITH".equalsIgnoreCase(firstKeyword)) {
            throw new DBCException("Only SELECT statements could produce execution plan");
        }
        return new OceanbasePlanJSON(session, query);
    }

    @Override
    public void serialize(Writer planData, DBCPlan plan) throws IOException, InvocationTargetException {
        serializeJson(planData, plan, dataSource.getInfo().getDriverName(), new DBCQueryPlannerSerialInfo() {

            @Override
            public String version() {
                return "json";
            }

            @Override
            public void addNodeProperties(DBCPlanNode node, JsonObject nodeJson) {
                JsonObject attributes = new JsonObject();
                OceanbasePlanNodeJSON jsNode = (OceanbasePlanNodeJSON) node;
                for (Map.Entry<String, String> e : jsNode.getNodeProps().entrySet()) {
                    attributes.add(e.getKey(), new JsonPrimitive(CommonUtils.notEmpty(e.getValue())));
                }
                nodeJson.add(PROP_ATTRIBUTES, attributes);
            }
        });
    }

    @Override
    public DBCPlan deserialize(Reader planData) throws IOException, InvocationTargetException {
        JsonObject jo = JsonParser.parseReader(planData).getAsJsonObject();

        String query = getQuery(jo);

        ExecutionPlanDeserializer<OceanbasePlanNodeJSON> loader = new ExecutionPlanDeserializer<>();
        List<OceanbasePlanNodeJSON> rootNodes = loader.loadRoot(dataSource, jo,
                (datasource, node, parent) -> new OceanbasePlanNodeJSON(parent, getNodeAttributes(node)));
        return new OceanbasePlanJSON(dataSource, query, rootNodes);
    }

    private static Map<String, String> getNodeAttributes(JsonObject nodeObject) {
        Map<String, String> attributes = new HashMap<>();

        JsonObject attrs = nodeObject.getAsJsonObject(PROP_ATTRIBUTES);
        for (Map.Entry<String, JsonElement> attr : attrs.entrySet()) {
            attributes.put(attr.getKey(), attr.getValue().getAsString());
        }

        return attributes;
    }

    @Override
    public DBPDataSource getDataSource() {
        // TODO Auto-generated method stub
        return this.dataSource;
    }

    @Override
    public DBCPlan planQueryExecution(DBCSession session, String query, DBCQueryPlannerConfiguration configuration)
            throws DBException {
        return explain((JDBCSession) session, query);
    }

    @Override
    public DBCPlanStyle getPlanStyle() {
        // TODO Auto-generated method stub
        return DBCPlanStyle.PLAN;
    }

}
