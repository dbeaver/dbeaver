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
package org.jkiss.dbeaver.model.lsm.sql.dialect;

import org.jkiss.dbeaver.model.lsm.LSMAnalyzer;
import org.jkiss.dbeaver.model.lsm.LSMAnalyzerFactory;
import org.jkiss.dbeaver.model.sql.SQLDialect;

public class SQLStandardAnalyzerFactory implements LSMAnalyzerFactory {

    public SQLStandardAnalyzerFactory() {
        // do nothing
    }

    @Override
    public LSMAnalyzer createAnalyzer(SQLDialect dialect) {
        return new SQLStandardAnalyzer(dialect);
    }
}
