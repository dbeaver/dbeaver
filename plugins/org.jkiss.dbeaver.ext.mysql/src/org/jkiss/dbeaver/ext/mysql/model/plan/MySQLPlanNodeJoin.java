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
package org.jkiss.dbeaver.ext.mysql.model.plan;

import java.util.ArrayList;

/**
 * MySQL execution plan node
 */
public class MySQLPlanNodeJoin extends MySQLPlanNodePlain {

    public MySQLPlanNodeJoin(MySQLPlanNodePlain parent, MySQLPlanNodePlain left, MySQLPlanNodePlain right) {
        super(parent, "JOIN");
        this.id = left.id;
        this.nested = new ArrayList<>(2);
        this.nested.add(left);
        this.nested.add(right);
        left.parent = this;
        right.parent = this;
    }

    public MySQLPlanNodeJoin(MySQLPlanNodePlain parent, MySQLPlanNodeJoin source) {
        super(parent, source);
    }

    @Override
    MySQLPlanNodePlain copyNode(MySQLPlanNodePlain parent) {
        return new MySQLPlanNodeJoin(parent, this);
    }
}
