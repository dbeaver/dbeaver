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

import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableRegular;

import static java.lang.String.format;

public class GreenplumWithClauseBuilder {

    public static String generateWithClause(PostgreTableRegular table, PostgreTableBase tableBase) {
        StringBuilder withClauseBuilder = new StringBuilder();

        if (tableSupportsAndHasOids(table) && tableIsGreenPlumWithRelOptions(table, tableBase)) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            withClauseBuilder.append(format(", %s\n)", String.join(", ", tableBase.getRelOptions())));
        } else if (tableSupportsAndHasOids(table)) {
            withClauseBuilder.append("\nWITH (\n\tOIDS=").append(table.isHasOids() ? "TRUE" : "FALSE");
            withClauseBuilder.append("\n)");
        } else if (tableIsGreenPlumWithRelOptions(table, tableBase)) {
            withClauseBuilder.append(format("\nWITH (\n\t%s\n)", String.join(", ", tableBase.getRelOptions())));
        }

        return withClauseBuilder.toString();
    }

    private static boolean tableSupportsAndHasOids(PostgreTableRegular table) {
        return table.getDataSource().getServerType().supportsOids() && table.isHasOids();
    }

    private static boolean tableIsGreenPlumWithRelOptions(PostgreTableRegular table, PostgreTableBase tableBase) {
        return table instanceof GreenplumTable && tableBase.getRelOptions() != null;
    }
}
