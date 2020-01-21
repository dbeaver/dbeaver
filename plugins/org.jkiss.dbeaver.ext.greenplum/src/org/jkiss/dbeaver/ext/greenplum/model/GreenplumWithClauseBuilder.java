/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Dmitriy Dubson (ddubson@pivotal.io)
 * Copyright (C) 2019 Gavin Shaw (gshaw@pivotal.io)
 * Copyright (C) 2019 Zach Marcin (zmarcin@pivotal.io)
 * Copyright (C) 2019 Nikhil Pawar (npawar@pivotal.io)
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
package org.jkiss.dbeaver.ext.greenplum.model;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;

import static java.lang.String.format;

public class GreenplumWithClauseBuilder {

    public static String generateWithClause(PostgreTable table, PostgreTableBase tableBase) {
        StringBuilder withClauseBuilder = new StringBuilder();

        if (tableSupportsAndHasOids(table) && tableIsGreenplumWithRelOptions(table, tableBase)) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            for (String option : tableBase.getRelOptions()) {
                withClauseBuilder.append(format(",\n\t%s", option));
            }
            withClauseBuilder.append("\n)");
        } else if (tableSupportsAndHasOids(table)) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            withClauseBuilder.append("\n)");
        } else if (tableIsGreenplumWithRelOptions(table, tableBase)) {
            String[] options = tableBase.getRelOptions();
            withClauseBuilder.append(format("\nWITH (\n\t%s", options[0]));
            for (int i = 1; i < options.length; i++) {
                String option = options[i];
                withClauseBuilder.append(format(",\n\t%s", option));
            }
            withClauseBuilder.append("\n)");
        }

        return withClauseBuilder.toString();
    }

    private static boolean tableSupportsAndHasOids(PostgreTable table) {
        return table.getDataSource().getServerType().supportsOids() && table.isHasOids();
    }

    private static boolean tableIsGreenplumWithRelOptions(PostgreTable table, PostgreTableBase tableBase) {
        return table instanceof GreenplumTable && tableBase.getRelOptions() != null;
    }
}
