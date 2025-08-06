/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AltibaseExplainPlanTest extends DBeaverUnitTest {
    @Test
    public void trcLogDetailMtrNode_1_Test() {
        String plan =
                "||----------------------------------------------------------" + "\n"
                        + "||-------------------------------------------------" + "\n"
                        + "||[[ PROJECTION GRAPH ]]" + "\n"
                        + "||-------------------------------------------------" + "\n"
                        + "||== Cost Information ==" + "\n"
                        + "||INPUT_RECORD_COUNT : 1137.77777778" + "\n"
                        + "||OUTPUT_RECORD_COUNT: 1137.77777778" + "\n"
                        + "||RECORD_SIZE        : 12" + "\n"
                        + "||SELECTIVITY        : 1" + "\n"
                        + "||GRAPH_ACCESS_COST  : 38.5706666667" + "\n"
                        + "||GRAPH_DISK_COST    : 0" + "\n"
                        + "||GRAPH_TOTAL_COST   : 38.5706666667" + "\n"
                        + "||TOTAL_ACCESS_COST  : 11210.6750981" + "\n"
                        + "||TOTAL_DISK_COST    : 0" + "\n"
                        + "||TOTAL_ALL_COST     : 11210.6750981" + "\n"
                        + "|  |-------------------------------------------------" + "\n"
                        + "|  |[[ SORTING GRAPH ]]"
                        + "|  |-------------------------------------------------" + "\n"
                        + "|  |== Cost Information ==" + "\n"
                        + "|  |INPUT_RECORD_COUNT : 1137.77777778" + "\n"
                        + "|  |OUTPUT_RECORD_COUNT: 1137.77777778" + "\n"
                        + "|  |RECORD_SIZE        : 12" + "\n"
                        + "|  |SELECTIVITY        : 1" + "\n"
                        + "|  |GRAPH_ACCESS_COST  : 237.832431457" + "\n"
                        + "|  |GRAPH_DISK_COST    : 0" + "\n"
                        + "|  |GRAPH_TOTAL_COST   : 237.832431457" + "\n"
                        + "|  |TOTAL_ACCESS_COST  : 11172.1044315" + "\n"
                        + "|  |TOTAL_DISK_COST    : 0" + "\n"
                        + "|  |TOTAL_ALL_COST     : 11172.1044315" + "\n"
                        + "|    |-------------------------------------------------" + "\n"
                        + "|    |[[ SELECTION GRAPH ]]" + "\n"
                        + "|    |-------------------------------------------------" + "\n"
                        + "|    |== Cost Information ==" + "\n"
                        + "|    |INPUT_RECORD_COUNT : 10240" + "\n"
                        + "|    |OUTPUT_RECORD_COUNT: 1137.77777778" + "\n"
                        + "|    |RECORD_SIZE        : 12" + "\n"
                        + "|    |SELECTIVITY        : 0.111111111111" + "\n"
                        + "|    |GRAPH_ACCESS_COST  : 10934.272" + "\n"
                        + "|    |GRAPH_DISK_COST    : 0" + "\n"
                        + "|    |GRAPH_TOTAL_COST   : 10934.272" + "\n"
                        + "|    |TOTAL_ACCESS_COST  : 10934.272" + "\n"
                        + "|    |TOTAL_DISK_COST    : 0" + "\n"
                        + "|    |TOTAL_ALL_COST     : 10934.272" + "\n"
                        + "|    |== Table Information ==" + "\n"
                        + "|    |TABLE NAME         : T50764" + "\n"
                        + "|    |  I1 : 100" + "\n"
                        + "|    |  I2 : 100" + "\n"
                        + "|    |  I3 : 100" + "\n"
                        + "|    |== Index Information ==" + "\n"
                        + "|    |== Access Method Information ==" + "\n"
                        + "|    |FULL SCAN" + "\n"
                        + "|    |  ACCESS_COST : 10934.272" + "\n"
                        + "|    |  DISK_COST   : 0" + "\n"
                        + "|    |  TOTAL_COST  : 10934.272" + "\n"
                        + "||----------------------------------------------------------" + "\n"
                        + "                   " + "\n"
                        + "-----------------------------------------------------------" + "\n"
                        + "PROJECT ( COLUMN_COUNT: 2, TUPLE_SIZE: 34, COST: 151146.46 )" + "\n"
                        + "[ TARGET INFO ]" + "\n"
                        + "sTargetColumn[0] : [2, 0],sTargetColumn->arg[X, X]" + "\n"
                        + "sTargetColumn[1] : [2, 1],sTargetColumn->arg[X, X]" + "\n"
                        + "sTargetColumn[2] : [2, 2],sTargetColumn->arg[X, X]" + "\n"
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
                        + "-----------------------------------------------------------" + "\n"
                        + "                   " + "\n"
                        + "-----------------------------------------------------------" + "\n"
                        + "PROJECT ( COLUMN_COUNT: 3, TUPLE_SIZE: 12, COST: 127.83 )" + "\n"
                        + "[ TARGET INFO ]" + "\n"
                        + "sTargetColumn[0] : [2, 0],sTargetColumn->arg[X, X]" + "\n"
                        + "sTargetColumn[1] : [2, 1],sTargetColumn->arg[X, X]" + "\n"
                        + "sTargetColumn[2] : [2, 2],sTargetColumn->arg[X, X]" + "\n"
                        + "[ RESULT ]" + "\n"
                        + "T50764.I1" + "\n"
                        + "T50764.I2" + "\n"
                        + "T50764.I3" + "\n"
                        + " SORT ( ITEM_SIZE: 16, ITEM_COUNT: 16, ACCESS: 16, COST: 127.39 )" + "\n"
                        + " [ myNode NODE INFO, SELF: 3, REF1: 2, REF2: -1 ]" + "\n"
                        + " sMtrNode[0] : src[2, ROWPTR],dst[3, 0]" + "\n"
                        + " sMtrNode[1] : src[2, *0],dst[3, 1]" + "\n"
                        + " [ RESULT ]" + "\n"
                        + " #T50764.I1" + "\n"
                        + " T50764.I2" + "\n"
                        + " T50764.I3" + "\n"
                        + "  SCAN ( TABLE: SYS.T50764, FULL SCAN, ACCESS: 16, COST: 124.68 )" + "\n"
                        + "  [ SELF NODE INFO, SELF: 2 ]" + "\n"
                        + "  [ RESULT ]" + "\n"
                        + "  T50764.I1" + "\n"
                        + "  T50764.I2" + "\n"
                        + "  T50764.I3" + "\n"
                        + "-----------------------------------------------------------" + "\n"
                        + "* SIMPLE QUERY PLAN";
        
        try {
            List<AltibasePlanNode> planNodeList = AltibasePlanBuilder.build(null, plan);
            assertTrue(planNodeList.size() > 0);
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}