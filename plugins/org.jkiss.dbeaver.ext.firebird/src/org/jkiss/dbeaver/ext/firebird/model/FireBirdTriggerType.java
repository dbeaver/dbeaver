/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.firebird.model;

/**
 * FireBirdDataSource
 */
public enum FireBirdTriggerType
{
    BEFORE_INSERT(1),
    AFTER_INSERT(2),
    BEFORE_UPDATE(3),
    AFTER_UPDATE(4),
    BEFORE_DELETE(5),
    AFTER_DELETE(6),
    BEFORE_INSERT_OR_UPDATE(17),
    AFTER_INSERT_OR_UPDATE(18),
    BEFORE_INSERT_OR_DELETE(25),
    AFTER_INSERT_OR_DELETE(26),
    BEFORE_UPDATE_OR_DELETE(27),
    AFTER_UPDATE_OR_DELETE(28),
    BEFORE_INSERT_OR_UPDATE_OR_DELETE(113),
    AFTER_INSERT_OR_UPDATE_OR_DELETE(114),
    ON_CONNECT(8192, true),
    ON_DISCONNECT(8193, true),
    ON_TRANSACTION_START(8194, true),
    ON_TRANSACTION_COMMIT(8195, true),
    ON_TRANSACTION_ROLLBACK (8196, true);

    private final int type;
    private final boolean dbEvent;

    FireBirdTriggerType(int type) {
        this(type, false);
    }

    FireBirdTriggerType(int type, boolean dbEvent) {
        this.type = type;
        this.dbEvent = dbEvent;
    }

    public String getDisplayName() {
        return name().replace('_', ' ');
    }

    public int getType() {
        return type;
    }

    public boolean isDbEvent() {
        return dbEvent;
    }

    static FireBirdTriggerType getByType(int type) {
        for (FireBirdTriggerType tt : values()) {
            if (tt.type == type) {
                return tt;
            }
        }
        return null;
    }

}
