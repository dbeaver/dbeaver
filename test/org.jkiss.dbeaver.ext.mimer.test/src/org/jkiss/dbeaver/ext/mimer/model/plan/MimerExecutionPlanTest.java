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

import org.jkiss.junit.DBeaverUnitTest;
import org.jkiss.utils.xml.XMLUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.StringReader;

/**
 * {@code getExplainText()}'s actual output wraps the plan in a {@code
 * <mimersql><explain version="..">} envelope the docs page doesn't show, and {@code bsql}'s
 * own "Start/End of explain result" framing strips before printing - only caught via a
 * standalone JDBC test against a live server. This is that exact captured output, verifying
 * {@link MimerExecutionPlan#findStatementRoot} unwraps it correctly.
 *
 * @author Mimer Information Technology
 */
public class MimerExecutionPlanTest extends DBeaverUnitTest {

    @Test
    public void findStatementRootUnwrapsMimersqlExplainEnvelope() throws Exception {
        String xml =
            "<?xml version=\"1.0\"?>" +
            "<mimersql>" +
            "<explain version=\"1.0\">" +
            "  <select cost=\"9\" hits=\"2\" visits=\"9\">" +
            "    <table name=\"mimer_store.countries c\" order=\"1\" index=\"cnt_primary_key\"" +
            "           scan=\"leadingKeys\" type=\"primary key\" cost=\"9\" hits=\"2\" visits=\"9\" rows=\"234\"/>" +
            "  </select>" +
            "</explain>" +
            "</mimersql>";
        Document document = XMLUtils.parseDocument(new StringReader(xml));

        Element statementRoot = MimerExecutionPlan.findStatementRoot(document.getDocumentElement());

        Assertions.assertEquals("select", statementRoot.getTagName());
        MimerPlanNode root = new MimerPlanNode(statementRoot, null);
        Assertions.assertEquals("Select", root.getNodeType());
        Assertions.assertEquals(1, root.getNested().size());
        Assertions.assertEquals("mimer_store.countries c", root.getNested().iterator().next().getNodeName());
    }

    @Test
    public void findStatementRootFallsBackToDocumentRootWhenEnvelopeIsMissing() throws Exception {
        // Defensive fallback in case a future driver version changes the envelope
        String xml = "<select cost=\"1\" hits=\"1\" visits=\"1\"/>";
        Document document = XMLUtils.parseDocument(new StringReader(xml));

        Element statementRoot = MimerExecutionPlan.findStatementRoot(document.getDocumentElement());

        Assertions.assertEquals("select", statementRoot.getTagName());
    }
}
