/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.altibase.model.plan;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.altibase.AltibaseConstants;
import org.jkiss.dbeaver.ext.altibase.model.AltibaseDataSource;

import java.util.ArrayList;
import java.util.List;

public class AltibasePlanBuilder {

    private static final Log log = Log.getLog(AltibasePlanBuilder.class);
    /**
     * Return plans tree structure based on the result from Altibase.
     */
    public static List<AltibasePlanNode> build(AltibaseDataSource dataSource, String planStr) throws IllegalStateException {
        List<AltibasePlanNode> rootNodes = new ArrayList<>();

        AltibasePlanNode node = null;
        AltibasePlanNode prevNode = null;
        String [] plans = planStr.split("\\n");
        int id = 0;
        int depth;

        // Altibase plan string is a depth-first traversal
        for (String plan : plans) {
            // The last condition is to skip the first and last line of the plan
            if (plan == null || plan.trim().length() < 1 || plan.startsWith("---")) {
                continue;
            }

            // No need line that starts with "* SIMPLE QUERY PLAN"
            if (plan.trim().startsWith("* SIMPLE")) {
                break;
            }

            // Count leading space to determine tree depth
            depth = plan.indexOf(plan.trim());

            plan = plan.trim();

            // root node
            if (depth == 0 && prevNode == null) {
                node = new AltibasePlanNode(dataSource, id++, 0, plan, null);
                rootNodes.add(node);
                // not root-node
            } else if (depth > 0 && prevNode != null) {
                // sibling
                if (prevNode.getDepth() == depth) {
                    node = new AltibasePlanNode(dataSource, id++, depth, plan, (AltibasePlanNode) prevNode.getParent());
                // prevNode is parent
                } else if (prevNode.getDepth() < depth) {
                    node = new AltibasePlanNode(dataSource, id++, depth, plan, prevNode);
                // prevNode.getDepth() > depth
                } else {
                    node = new AltibasePlanNode(dataSource, id++, depth, plan, prevNode.getParentNodeAtDepth(depth));
                }
            } else { 
                throw new IllegalStateException("Plan parsing error [depth: " + depth + "]: " + plan + "\n" + planStr);
            }

            prevNode = node;
        }

        return rootNodes;
    }

    /**
     * Test code
     */
    public static void main(String[] args) {
        String plan = "-----------------------------------------------------------" + "\n"
                + "PROJECT ( COLUMN_COUNT: 2, TUPLE_SIZE: 34, COST: 151146.46 )" + "\n"
                + " JOIN ( METHOD: NL, COST: 148444.31 )" + "\n"
                + "  SCAN ( TABLE: CUSTOMERS, FULL SCAN, ACCESS: ??, COST: 116.76 )" + "\n"
                + "  VIEW ( ORDERS_T, ACCESS: ??, COST: 14.49 )" + "\n"
                + "   PROJECT ( COLUMN_COUNT: 6, TUPLE_SIZE: 48, COST: 2.81 )" + "\n"
                + "    VIEW ( ACCESS: ??, COST: 2.02 )" + "\n"
                + "     BAG-UNION" + "\n"
                + "      PROJECT ( COLUMN_COUNT: 6, TUPLE_SIZE: 48, COST: 0.67 )" + "\n"
                + "       SCAN ( TABLE: ORDERS ORDERS_01, INDEX: SYS.ODR_IDX2, RANGE SCAN, ACCESS: ??, COST: 0.41 )" + "\n"
                + "        [ VARIABLE KEY ]" + "\n"
                + "        OR" + "\n"
                + "         AND" + "\n"
                + "          CUSTOMERS.CNO = ORDERS_01.CNO" + "\n"
                + "        [ FILTER ]" + "\n"
                + "        ORDERS_01.QTY >= 10000" + "\n"
                + "      PROJECT ( COLUMN_COUNT: 6, TUPLE_SIZE: 48, COST: 0.67 )" + "\n"
                + "       SCAN ( TABLE: ORDERS ORDERS_02, INDEX: SYS.ODR_IDX2, RANGE SCAN, ACCESS: ??, COST: 0.41 )" + "\n"
                + "        [ VARIABLE KEY ]" + "\n"
                + "        OR" + "\n"
                + "         AND" + "\n"
                + "          CUSTOMERS.CNO = ORDERS_02.CNO" + "\n"
                + "        [ FILTER ]" + "\n"
                + "        ORDERS_02.QTY >= 10000" + "\n"
                + "      PROJECT ( COLUMN_COUNT: 6, TUPLE_SIZE: 48, COST: 0.67 )" + "\n"
                + "       SCAN ( TABLE: ORDERS ORDERS_03, INDEX: ODR_IDX2, RANGE SCAN, ACCESS: ??, COST: 0.41 ) " + "\n"
                + "        [ VARIABLE KEY ]" + "\n"
                + "        OR" + "\n"
                + "         AND" + "\n"
                + "          CUSTOMERS.CNO = ORDERS_03.CNO" + "\n"
                + "        [ FILTER ]" + "\n"
                + "        ORDERS_03.QTY >= 10000" + "\n"
                + "-----------------------------------------------------------"
                + "* SIMPLE QUERY PLAN";

        try {
            
            if (!AltibaseConstants.DEBUG) {
                return;
            }
            
            List<AltibasePlanNode> rootNodes  = AltibasePlanBuilder.build(null, plan);

            log.debug(rootNodes.get(0).toString4Debug());
        } catch (Exception e) {
            log.warn(e.getMessage());
        }
    }
}
