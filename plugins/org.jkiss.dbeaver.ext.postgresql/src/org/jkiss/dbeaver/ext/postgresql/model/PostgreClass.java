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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.struct.DBSEntity;

/**
 * PostgreClass
 */
public interface PostgreClass extends PostgreObject, DBSEntity, DBPRefreshableObject
{
    class RelKind {
        public static final RelKind r = new RelKind("r");  // ordinary table
        public static final RelKind i = new RelKind("i");  // index
        public static final RelKind S = new RelKind("S");  // sequence
        public static final RelKind v = new RelKind("v");  // view
        public static final RelKind m = new RelKind("m");  // materialized view
        public static final RelKind c = new RelKind("c");  // composite type
        public static final RelKind t = new RelKind("t");  // TOAST table
        public static final RelKind f = new RelKind("f");  // = foreign table
        public static final RelKind p = new RelKind("p");  // partitionedtable
        // Redshift
        public static final RelKind e = new RelKind("e");
        public static final RelKind s = new RelKind("s");;

        private final String code;

        public RelKind(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        static RelKind valueOf(String code) {
            try {
                return (RelKind) RelKind.class.getField(code).get(null);
            } catch (Throwable e1) {
                return new RelKind(code);
            }
        }
    }

    @NotNull
    PostgreDataSource getDataSource();

}
