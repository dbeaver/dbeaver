/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreServerExtension;
import org.jkiss.dbeaver.ext.postgresql.model.impls.*;
import org.jkiss.dbeaver.ext.postgresql.model.impls.greenplum.PostgreServerGreenplum;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;
import org.jkiss.dbeaver.ext.postgresql.model.impls.yellowbrick.PostgreServerYellowBrick;

/**
 * Database type
 */
public enum PostgreServerType {

    POSTGRESQL(PostgreServerPostgreSQL.class),
    GREENPLUM(PostgreServerGreenplum.class),
    REDSHIFT(PostgreServerRedshift.class),
    TIMESCALE(PostgreServerTimescale.class),
    YELLOWBRICK(PostgreServerYellowBrick.class),
    COCKROACH(PostgreServerCockroachDB.class),
    OTHER(PostgreServerPostgreSQL.class);

    private final Class<? extends PostgreServerExtension> implClass;

    PostgreServerType(Class<? extends PostgreServerExtension> implClass) {
        this.implClass = implClass;
    }

    public Class<? extends PostgreServerExtension> getImplClass() {
        return implClass;
    }
}
