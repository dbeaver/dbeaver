package org.jkiss.dbeaver.model.impl.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCQueryPlannerDeSerialInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ExecutionPlanDeserializer<NODE extends DBCPlanNode> {

    public List<NODE> loadRoot(DBPDataSource datasource, JsonObject plan, DBCQueryPlannerDeSerialInfo<NODE> info) throws InvocationTargetException {
        
        final String signature = plan.get(AbstractExecutionPlanSerializer.PROP_SIGNATURE).getAsString();
        final String currSignature = datasource.getInfo().getDriverName();
        
        if (!signature.equals(currSignature)) {
            throw new InvocationTargetException(new Throwable(String.format("Incorrect plan signature found - %s, expected - %s", signature,currSignature)));
        }
        
        final List<NODE> nodes = new ArrayList<>(1);
        plan.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_NODES).forEach((e) -> {
            nodes.add(loadNode(datasource, e.getAsJsonObject(), null, info));
        });
        return nodes;
    }

    private NODE loadNode(DBPDataSource dataSource, JsonObject nodeObject, NODE parent,
            DBCQueryPlannerDeSerialInfo<NODE> info) {
        
        NODE node = info.createNode(dataSource, nodeObject, parent);
        JsonArray childs = nodeObject.getAsJsonArray(AbstractExecutionPlanSerializer.PROP_CHILD);
        
        if (childs != null) {
            childs.forEach((e) -> {
                ((Collection<NODE>) node.getNested()).add(loadNode(dataSource, e.getAsJsonObject(), node, info));
            });
        }
        
        return node;
    }

}
