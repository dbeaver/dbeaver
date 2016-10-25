/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.lang.parser;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.rules.IToken;
import org.jkiss.dbeaver.lang.*;
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
