/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.lang.parser;

import org.eclipse.jface.text.Document;
import org.jkiss.dbeaver.lang.SCMRoot;
import org.jkiss.dbeaver.lang.SCMSourceParser;
import org.jkiss.dbeaver.lang.SCMSourceScanner;
import org.jkiss.dbeaver.lang.sql.SQLNodeParser;

/**
 * ParseUtils.
 */
public class ParseUtils {

    public static SCMRoot parseDocument(Document document, SCMSourceParser nodeParser) {
        SCMSourceScanner scanner = nodeParser.createScanner(document);
        SCMRoot documentNode = new SCMRoot(nodeParser, scanner);

        documentNode.parseComposite(scanner);

        return documentNode;
    }


    public static void main(String[] args) {
        System.out.println("Test SCM parser");

        String sql = "SELECT * FROM SCHEMA.TABLE WHERE COL1 <> 100 AND COL2 = 'TEST'";

        SCMRoot nodeTree = ParseUtils.parseDocument(new Document(sql), new SQLNodeParser());

        System.out.println(nodeTree);
    }
}
