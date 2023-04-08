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
package org.jkiss.dbeaver.model.lsm.sql.impl.syntax;

import org.jkiss.dbeaver.model.lsm.LSMContext;
import org.jkiss.dbeaver.model.lsm.LSMElement;
import org.jkiss.dbeaver.model.lsm.LSMEngine;

import java.io.StringReader;


public class TestEngine {

    public static void main(String[] args) throws Exception {
        LSMContext context = new LSMContext(
            "SQL Parser",
            new Sql92Lexer(null),
            new Sql92Parser(null)
        );

        LSMEngine engine = new LSMEngine(context);
        LSMElement result = engine.parseText(new StringReader("SELECT col_name FROM sch.table_name;"));
        System.out.println(result);
    }
}