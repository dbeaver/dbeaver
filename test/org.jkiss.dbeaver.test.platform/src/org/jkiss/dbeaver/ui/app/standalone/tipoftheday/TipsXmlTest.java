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
package org.jkiss.dbeaver.ui.app.standalone.tipoftheday;

import org.eclipse.core.runtime.Platform;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * We are moving away from Eclipse Forms because they are not so consistent with theming.
 * Current TipOfTheDay implementation supports only subset of formatting tags actually used in our tips.xml,
 * which is reflected in the corresponding tips.xsd schema.
 * In case of any other tags or attributes required, remember to keep both schema and logic implementation in consistent state.
 * In case of introducing more xml files containing tips, don't forget to introduce corresponding test cases validating them.
 */
class TipsXmlTest extends DBeaverUnitTest {

    @Test
    public void tipsXmlMatchesSchema() throws Exception {
        try (InputStream tipsSchemaStream = TipsXmlHandler.openTipsSchemaFile(); InputStream tipsFileStream = TipsXmlHandler.openTipsFile()) {
            StreamSource schemaSource = new StreamSource(tipsSchemaStream);
            StreamSource tipsSource = new StreamSource(tipsFileStream);
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(schemaSource)
                .newValidator()
                .validate(tipsSource);
        }
    }
}
