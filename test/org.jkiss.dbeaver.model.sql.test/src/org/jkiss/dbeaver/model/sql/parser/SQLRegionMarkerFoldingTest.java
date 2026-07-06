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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLDialectMetadataRegistry;
import org.jkiss.dbeaver.model.sql.SQLPartitionScanner;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SQLRegionMarkerFoldingTest extends DBeaverUnitTest {

    private static final String NESTED_REGIONS_SCRIPT = """
        --region OUTER
        SELECT 0
        --region INNER1
        SELECT 1
        --region INNER11
        SELECT 2
        --endregion
        SELECT 3
        --endregion
        SELECT 4
        --endregion
        SELECT 5
        """;

    @Test
    public void nestedRegionsHaveHierarchicalKeysAndStartOffsets() throws DBException {
        Document document = createPartitionedDocument(NESTED_REGIONS_SCRIPT);
        List<SQLRegionMarkerFolding.RegionFold> regions = SQLRegionMarkerFolding.scanFoldableRegions(document);
        Map<String, SQLRegionMarkerFolding.RegionFold> byKey = regions.stream()
            .collect(Collectors.toMap(SQLRegionMarkerFolding.RegionFold::regionKey, Function.identity()));

        Assertions.assertEquals(3, regions.size());
        Assertions.assertTrue(byKey.containsKey("OUTER"));
        Assertions.assertTrue(byKey.containsKey("OUTER/INNER1"));
        Assertions.assertTrue(byKey.containsKey("OUTER/INNER1/INNER11"));

        for (String markerLine : List.of("--region OUTER", "--region INNER1", "--region INNER11")) {
            int expectedOffset = document.get().indexOf(markerLine);
            String key = markerLine.replace("--region ", "").trim();
            if (!key.equals("OUTER")) {
                if (key.equals("INNER1")) {
                    key = "OUTER/INNER1";
                } else {
                    key = "OUTER/INNER1/INNER11";
                }
            }
            Assertions.assertEquals(expectedOffset, byKey.get(key).offset(), key);
        }
    }

    @Test
    public void orphanEndRegionIsIgnored() {
        Document document = new Document("""
            SELECT 1
            --endregion
            SELECT 2
            """);
        List<SQLRegionMarkerFolding.RegionFold> regions = SQLRegionMarkerFolding.scanRegions(document);
        Assertions.assertTrue(regions.isEmpty());
    }

    @Test
    public void unclosedRegionIsNotReturned() {
        Document document = new Document("""
            --region OPEN
            SELECT 1
            SELECT 2
            """);
        List<SQLRegionMarkerFolding.RegionFold> regions = SQLRegionMarkerFolding.scanRegions(document);
        Assertions.assertTrue(regions.isEmpty());
    }

    @Test
    public void regionMarkerInsideStringIsIgnored() throws DBException {
        Document document = createPartitionedDocument("SELECT '--region' FROM t;\n");
        List<SQLRegionMarkerFolding.RegionFold> regions = SQLRegionMarkerFolding.scanRegions(document);
        Assertions.assertTrue(regions.isEmpty());
    }

    @Test
    public void markerOnlyPairIsFoldableWhenItSpansTwoLines() {
        Document document = new Document("""
            --region A
            --endregion
            """);
        List<SQLRegionMarkerFolding.RegionFold> allRegions = SQLRegionMarkerFolding.scanRegions(document);
        Assertions.assertEquals(1, allRegions.size());
        Assertions.assertEquals(2, SQLRegionMarkerFolding.getRegionNumberOfLines(document, allRegions.getFirst()));
        Assertions.assertEquals(1, SQLRegionMarkerFolding.scanFoldableRegions(document).size());
    }

    @Test
    public void renamedRegionMarkerProducesDifferentKey() {
        Document documentBefore = new Document("""
            --region INNER1
            SELECT 1
            --endregion
            """);
        Document documentAfter = new Document("""
            --region INNER2
            SELECT 1
            --endregion
            """);
        List<SQLRegionMarkerFolding.RegionFold> before = SQLRegionMarkerFolding.scanRegions(documentBefore);
        List<SQLRegionMarkerFolding.RegionFold> after = SQLRegionMarkerFolding.scanRegions(documentAfter);
        Assertions.assertEquals("INNER1", before.getFirst().regionKey());
        Assertions.assertEquals("INNER2", after.getFirst().regionKey());
    }

    private static Document createPartitionedDocument(String sql) throws DBException {
        SQLDialectMetadataRegistry registry = DBWorkbench.getPlatform().getSQLDialectRegistry();
        SQLDialect dialect = registry.getDialect("generic").createInstance();
        SQLSyntaxManager syntaxManager = new SQLSyntaxManager();
        syntaxManager.init(dialect, DBWorkbench.getPlatform().getPreferenceStore());
        SQLRuleManager ruleManager = new SQLRuleManager(syntaxManager);
        ruleManager.loadRules();
        Document document = new Document(sql);
        FastPartitioner partitioner = new FastPartitioner(
            new SQLPartitionScanner(null, dialect, ruleManager),
            SQLParserPartitions.SQL_CONTENT_TYPES);
        partitioner.connect(document);
        document.setDocumentPartitioner(SQLParserPartitions.SQL_PARTITIONING, partitioner);
        return document;
    }
}
