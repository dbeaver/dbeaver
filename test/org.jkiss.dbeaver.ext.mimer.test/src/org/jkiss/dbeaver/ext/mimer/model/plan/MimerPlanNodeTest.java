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
package org.jkiss.dbeaver.ext.mimer.model.plan;

import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.xml.XMLUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.io.StringReader;
import java.util.Iterator;

/**
 * Parses real {@code getExplainText()} XML samples captured off a live server (11.00.0009)
 * against {@link MimerPlanNode}, covering the two spots that turned out to differ from the
 * docs page (see that class's Javadoc): a correlated {@code <subselect>} sitting as a
 * *sibling* of a {@code <table>} rather than nested inside a join, the all-lowercase
 * {@code indexlookuponly} attribute, and a {@code <tempTable>} that omits {@code hits}
 * entirely.
 *
 * @author Mimer Information Technology
 */
public class MimerPlanNodeTest extends DBeaverUnitTest {

    @Test
    public void correlatedSubselectIsSiblingOfOuterTable() throws Exception {
        // SELECT c.surname FROM customers c
        // WHERE EXISTS (SELECT 1 FROM orders o WHERE o.customer_id = c.customer_id)
        String xml =
            "<select cost=\"0\" hits=\"0\" visits=\"0\">" +
            "  <table name=\"mimer_store.customers c\" order=\"1\" index=\"cst_primary_key\"" +
            "         scan=\"sequential\" type=\"primary key\" cost=\"0\" hits=\"0\" visits=\"0\" rows=\"0\"/>" +
            "  <subselect cost=\"0\" hits=\"1\" visits=\"0\">" +
            "    <table name=\"mimer_store.orders o\" order=\"2\" index=\"ord_customers\"" +
            "           scan=\"leadingKeys\" type=\"foreign key index\" indexlookuponly=\"true\"" +
            "           cost=\"0\" hits=\"1\" visits=\"0\" rows=\"0\"/>" +
            "  </subselect>" +
            "</select>";
        MimerPlanNode root = parse(xml);

        Assertions.assertEquals("Select", root.getNodeType());
        Assertions.assertEquals(2, root.getNested().size());

        Iterator<MimerPlanNode> children = root.getNested().iterator();
        MimerPlanNode outerTable = children.next();
        Assertions.assertEquals("mimer_store.customers c", outerTable.getNodeName());
        Assertions.assertEquals(DBCPlanNodeKind.TABLE_SCAN, outerTable.getNodeKind());

        MimerPlanNode subselect = children.next();
        Assertions.assertEquals(DBCPlanNodeKind.SELECT, subselect.getNodeKind());
        Assertions.assertEquals(1, subselect.getNested().size());

        MimerPlanNode innerTable = subselect.getNested().iterator().next();
        Assertions.assertEquals(DBCPlanNodeKind.INDEX_SCAN, innerTable.getNodeKind());
        Assertions.assertEquals(
            "foreign key index via ord_customers (index-only)",
            innerTable.getNodeDescription());
    }

    @Test
    public void distinctTempTableCanOmitHits() throws Exception {
        // SELECT DISTINCT country_code FROM customers
        String xml =
            "<select cost=\"0\" hits=\"0\" visits=\"0\">" +
            "  <tempTable cost=\"0\" class=\"TempTableDistinct\" tempWrites=\"0\">" +
            "    <table name=\"mimer_store.customers\" order=\"1\" index=\"cst_primary_key\"" +
            "           scan=\"sequential\" type=\"primary key\" cost=\"0\" hits=\"0\" visits=\"0\" rows=\"0\"/>" +
            "  </tempTable>" +
            "</select>";
        MimerPlanNode root = parse(xml);

        MimerPlanNode tempTable = root.getNested().iterator().next();
        Assertions.assertEquals("Temp Table (Distinct)", tempTable.getNodeType());
        Assertions.assertEquals(DBCPlanNodeKind.SET, tempTable.getNodeKind());
        Assertions.assertNull(tempTable.getHits());
        Assertions.assertEquals("0", tempTable.getTempWrites());
    }

    private static MimerPlanNode parse(String xml) throws Exception {
        Document document = XMLUtils.parseDocument(new StringReader(xml));
        return new MimerPlanNode(document.getDocumentElement(), null);
    }
}
