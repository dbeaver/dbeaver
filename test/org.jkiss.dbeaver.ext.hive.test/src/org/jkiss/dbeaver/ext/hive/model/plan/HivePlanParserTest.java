/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.hive.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.runtime.properties.PropertyCollector;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

class HivePlanParserTest extends DBeaverUnitTest {
    private static final String LEGACY_PLAN = """
        {
          "STAGE DEPENDENCIES": {
            "Stage-1": {"ROOT STAGE": "TRUE"},
            "Stage-0": {"DEPENDENT STAGES": "Stage-1"}
          },
          "STAGE PLANS": {
            "Stage-1": {
              "Map Reduce": {
                "Alias -> Map Operator Tree:": {
                  "src": {
                    "TableScan": {
                      "OperatorId:": "TS_0",
                      "Statistics:": "Num rows: 42 Data size: 1024 Basic stats: COMPLETE Column stats: COMPLETE",
                      "children": {
                        "Filter Operator": {
                          "OperatorId:": "FIL_1",
                          "predicate:": "key > 10"
                        }
                      }
                    }
                  }
                }
              }
            },
            "Stage-0": {
              "Fetch Operator": {
                "limit:": "10",
                "Processor Tree:": {
                  "ListSink": {"OperatorId:": "LIST_SINK_2"}
                }
              }
            }
          }
        }
        """;

    private static final String HIVE_4_PLAN = """
        {
          "CBOPlan": "{\\"rels\\":[]}",
          "optimizedSQL": "SELECT * FROM default.src",
          "cboInfo": "Plan optimized by CBO.",
          "STAGE DEPENDENCIES": {
            "Stage-1": {"ROOT STAGE": "TRUE"}
          },
          "STAGE PLANS": {
            "Stage-1": {
              "Tez": {
                "Vertices:": {
                  "Map 1": {
                    "Map Operator Tree:": [
                      {
                        "TableScan": {
                          "table:": "orders",
                          "OperatorId:": "TS_0",
                          "Statistics:": "Num rows: 500 Data size: 47000 Basic stats: COMPLETE Column stats: PARTIAL",
                          "children": [
                            {
                              "Select Operator": {
                                "OperatorId:": "SEL_1",
                                "Statistics:": "Num rows: 250 Data size: 23500 Basic stats: COMPLETE Column stats: PARTIAL",
                                "children": {
                                  "Map Join Operator": {
                                    "condition map:": [{"": "Inner Join 0 to 1"}],
                                    "keys:": {
                                      "0": "customer_id (type: bigint)",
                                      "1": "id (type: bigint)"
                                    },
                                    "input vertices:": {"1": "Map 4"},
                                    "Statistics:": "Num rows: 200 Data size: 18800 Basic stats: COMPLETE Column stats: PARTIAL",
                                    "children": {
                                      "Group By Operator": {
                                        "aggregations:": ["sum(amount)"],
                                        "keys:": "customer_id (type: bigint)",
                                        "mode:": "hash",
                                        "Statistics:": "Num rows: 20 Data size: 640 Basic stats: COMPLETE Column stats: COMPLETE",
                                        "children": {
                                          "Reduce Output Operator": {
                                            "key expressions:": "customer_id (type: bigint)",
                                            "Map-reduce partition columns:": "customer_id (type: bigint)",
                                            "sort order:": "+",
                                            "null sort order:": "a",
                                            "outputname:": "Reducer 2",
                                            "Statistics:": "Num rows: 20 Data size: 640 Basic stats: COMPLETE Column stats: COMPLETE"
                                          }
                                        }
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          ]
                        }
                      }
                    ]
                  },
                  "Map 4": {
                    "Map Operator Tree:": [
                      {
                        "TableScan": {
                          "table:": "customers",
                          "Statistics:": "Num rows: 10 Data size: 800 Basic stats: COMPLETE Column stats: COMPLETE"
                        }
                      }
                    ]
                  },
                  "Reducer 2": {
                    "Reduce Operator Tree:": {
                      "Select Operator": {
                        "Statistics:": "Num rows: 20 Data size: 640 Basic stats: COMPLETE Column stats: COMPLETE"
                      }
                    }
                  },
                  "Union 3": {}
                },
                "Edges:": {
                  "Map 1": {"parent": "Map 4", "type": "BROADCAST_EDGE"},
                  "Reducer 2": [
                    {"parent": "Map 1", "type": "SIMPLE_EDGE"},
                    {"parent": "Map 4", "type": "CUSTOM_SIMPLE_EDGE"}
                  ],
                  "Union 3": {"parent": "Reducer 2", "type": "CONTAINS"}
                }
              }
            }
          }
        }
        """;

    @Test
    void parsesLegacyStagesOperatorsAndMetrics() {
        List<HivePlanNode> roots = new HivePlanParser().parse(LEGACY_PLAN);

        Assertions.assertEquals(2, roots.size());
        Assertions.assertEquals("Stage-1", roots.get(0).getNodeName());
        Assertions.assertEquals("TRUE", roots.get(0).getNodeProperties().get("ROOT STAGE"));
        Assertions.assertEquals("Stage-1", roots.get(1).getNodeProperties().get("DEPENDENT STAGES"));

        HivePlanNode tableScan = findNode(roots, "TableScan");
        Assertions.assertNotNull(tableScan);
        Assertions.assertEquals("src", tableScan.getNodeName());
        Assertions.assertEquals("src", tableScan.getEntity());
        Assertions.assertEquals("TS_0", tableScan.getNodeProperties().get("OperatorId:"));
        Assertions.assertEquals("src", tableScan.getNodeProperties().get("alias:"));
        Assertions.assertEquals(DBCPlanNodeKind.TABLE_SCAN, tableScan.getNodeKind());
        Assertions.assertEquals(42.0, tableScan.getNodeRowCount().doubleValue());
        Assertions.assertEquals(1024L, tableScan.getEstimatedDataSize());
        Assertions.assertEquals("COMPLETE / COMPLETE", tableScan.getStatisticsStatus());
        Assertions.assertNull(tableScan.getNodeCost());

        HivePlanNode filter = findNode(tableScan.getNested(), "Filter Operator");
        Assertions.assertNotNull(filter);
        Assertions.assertEquals("FIL_1", filter.getNodeName());
        Assertions.assertEquals("key > 10", filter.getNodeCondition());
        Assertions.assertEquals(DBCPlanNodeKind.FILTER, filter.getNodeKind());

        Assertions.assertEquals(DBCPlanNodeKind.DEFAULT, new HivePlanNode(null, "Processor Tree", null).getNodeKind());
        Assertions.assertEquals(DBCPlanNodeKind.DEFAULT, new HivePlanNode(null, "Reduce Output Operator", null).getNodeKind());
        Assertions.assertNull(findNode(roots, "Alias -> Map Operator Tree"));
        Assertions.assertNull(findNode(roots, "src"));
        Assertions.assertNull(findNode(roots, "Processor Tree"));
    }

    @Test
    void parsesHive4MetadataTezAndArrayChildren() {
        List<HivePlanNode> roots = new HivePlanParser().parse(HIVE_4_PLAN);

        Assertions.assertEquals(1, roots.size());
        Assertions.assertEquals("Plan optimized by CBO.", roots.getFirst().getNodeProperties().get("cboInfo"));
        Assertions.assertEquals("SELECT * FROM default.src", roots.getFirst().getNodeProperties().get("optimizedSQL"));

        HivePlanNode tez = findNodeByName(roots, "Tez");
        Assertions.assertNotNull(tez);
        Assertions.assertEquals("Execution Engine", tez.getNodeType());
        Assertions.assertEquals("Tez", tez.getEntity());
        Assertions.assertNotNull(tez.getNodeProperties().get("Edges:"));

        HivePlanNode map = findNodeByName(roots, "Map 1");
        Assertions.assertNotNull(map);
        Assertions.assertEquals("Vertex", map.getNodeType());
        Assertions.assertEquals("Map 1", map.getEntity());
        Assertions.assertEquals("Map 4 (BROADCAST_EDGE)", map.getInputs());
        Assertions.assertNull(findNode(roots, "Vertices"));
        Assertions.assertNull(findNode(roots, "Map Operator Tree"));

        HivePlanNode tableScan = findNode(roots, "TableScan");
        Assertions.assertNotNull(tableScan);
        Assertions.assertEquals("orders", tableScan.getEntity());
        Assertions.assertEquals(500L, tableScan.getNodeRowCount());
        Assertions.assertEquals(500.0, tableScan.getNodeRowCount().doubleValue());
        Assertions.assertEquals(47000L, tableScan.getEstimatedDataSize());
        Assertions.assertEquals("COMPLETE / PARTIAL", tableScan.getStatisticsStatus());

        HivePlanNode select = findNode(tableScan.getNested(), "Select Operator");
        Assertions.assertNotNull(select);
        Assertions.assertEquals(250.0, select.getNodeRowCount().doubleValue());
        Assertions.assertEquals(DBCPlanNodeKind.SELECT, select.getNodeKind());

        HivePlanNode mapJoin = findNode(roots, "Map Join Operator");
        Assertions.assertNotNull(mapJoin);
        Assertions.assertEquals("Inner Join 0 to 1", mapJoin.getNodeCondition());
        Assertions.assertEquals(
            "keys: 0=customer_id (type: bigint); 1=id (type: bigint) | input vertices: 1=Map 4",
            mapJoin.getNodeDetails());

        HivePlanNode groupBy = findNode(roots, "Group By Operator");
        Assertions.assertNotNull(groupBy);
        Assertions.assertEquals(
            "keys: customer_id (type: bigint) | aggregations: sum(amount) | mode: hash",
            groupBy.getNodeDetails());

        HivePlanNode reduceOutput = findNode(roots, "Reduce Output Operator");
        Assertions.assertNotNull(reduceOutput);
        Assertions.assertEquals("Reducer 2", reduceOutput.getEntity());
        Assertions.assertEquals(
            "key expressions: customer_id (type: bigint) | " +
                "Map-reduce partition columns: customer_id (type: bigint) | sort order: + | null sort order: a",
            reduceOutput.getNodeDetails());

        HivePlanNode reducer = findNodeByTypeAndName(roots, "Vertex", "Reducer 2");
        Assertions.assertNotNull(reducer);
        Assertions.assertEquals("Map 1 (SIMPLE_EDGE); Map 4 (CUSTOM_SIMPLE_EDGE)", reducer.getInputs());

        HivePlanNode union = findNodeByTypeAndName(roots, "Vertex", "Union 3");
        Assertions.assertNotNull(union);
        Assertions.assertEquals("Reducer 2 (CONTAINS)", union.getInputs());
    }

    @Test
    void rejectsInvalidOrUnstructuredPlans() {
        HivePlanParser parser = new HivePlanParser();

        Assertions.assertThrows(RuntimeException.class, () -> parser.parse("not json"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> parser.parse("{}"));
    }

    @Test
    void createsExplainFormattedQuery() {
        HiveExecutionPlan plan = new HiveExecutionPlan("SELECT * FROM src", new HivePlanParser());

        Assertions.assertEquals("EXPLAIN FORMATTED SELECT * FROM src", plan.getPlanQueryString());
    }

    @Test
    void parsesRuntimeStatisticsAndPartitionDetails() {
        HivePlanNode tableScan = new HivePlanNode(null, "TableScan", null);
        tableScan.addProperty(
            "Statistics:",
            "(RUNTIME) Num rows: 500/513 Data size: 89000 Erasure files: 1 " +
                "Basic stats: COMPLETE Column stats: PARTIAL");

        Assertions.assertEquals(500.0, tableScan.getNodeRowCount().doubleValue());
        Assertions.assertEquals(89000L, tableScan.getEstimatedDataSize());
        Assertions.assertEquals("COMPLETE / PARTIAL", tableScan.getStatisticsStatus());

        HivePlanNode pruning = new HivePlanNode(null, "Dynamic Partitioning Event Operator", null);
        pruning.addProperty("Partition key expr:", "ds");
        pruning.addProperty("Target column:", "ds (string)");
        pruning.addProperty("Target Input:", "orders");
        pruning.addProperty("Target Vertex:", "Map 1");
        Assertions.assertEquals(
            "Partition key expr: ds | Target column: ds (string) | Target Input: orders | Target Vertex: Map 1",
            pruning.getNodeDetails());

        HivePlanNode partitionValues = new HivePlanNode(null, "partition values", null);
        partitionValues.addProperty("ds", "2026-01-01");
        partitionValues.addProperty("region", "EMEA");
        Assertions.assertEquals("ds=2026-01-01; region=EMEA", partitionValues.getNodeDetails());
    }

    @Test
    void exposesViewablePropertiesThroughPropertyCollector() {
        HivePlanNode tableScan = findNode(new HivePlanParser().parse(LEGACY_PLAN), "TableScan");
        Assertions.assertNotNull(tableScan);
        PropertyCollector collector = new PropertyCollector(tableScan, false);

        Assertions.assertTrue(collector.collectProperties());
        Assertions.assertEquals("TableScan", collector.getPropertyValue(null, "nodeType"));
        Assertions.assertEquals("src", collector.getPropertyValue(null, "entity"));
        Assertions.assertEquals(42.0, ((Number) collector.getPropertyValue(null, "nodeRowCount")).doubleValue());
        Assertions.assertEquals("1K", collector.getPropertyValue(null, "estimatedDataSize"));
        Assertions.assertEquals("COMPLETE / COMPLETE", collector.getPropertyValue(null, "statisticsStatus"));
        Assertions.assertTrue(
            List.of(collector.getProperties()).stream()
                .noneMatch(property -> "nodeCost".equals(property.getId())));

        HivePlanNode filter = findNode(tableScan.getNested(), "Filter Operator");
        Assertions.assertNotNull(filter);
        PropertyCollector filterCollector = new PropertyCollector(filter, false);
        Assertions.assertTrue(filterCollector.collectProperties());
        Assertions.assertEquals("key > 10", filterCollector.getPropertyValue(null, "nodeCondition"));
    }

    @Nullable
    private static HivePlanNode findNode(
        @NotNull Collection<HivePlanNode> nodes,
        @NotNull String type
    ) {
        for (HivePlanNode node : nodes) {
            if (type.equals(node.getNodeType())) {
                return node;
            }
            HivePlanNode nested = findNode(node.getNested(), type);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    @Nullable
    private static HivePlanNode findNodeByName(
        @NotNull Collection<HivePlanNode> nodes,
        @NotNull String name
    ) {
        for (HivePlanNode node : nodes) {
            if (name.equals(node.getNodeName())) {
                return node;
            }
            HivePlanNode nested = findNodeByName(node.getNested(), name);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    @Nullable
    private static HivePlanNode findNodeByTypeAndName(
        @NotNull Collection<HivePlanNode> nodes,
        @NotNull String type,
        @NotNull String name
    ) {
        for (HivePlanNode node : nodes) {
            if (type.equals(node.getNodeType()) && name.equals(node.getNodeName())) {
                return node;
            }
            HivePlanNode nested = findNodeByTypeAndName(node.getNested(), type, name);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
