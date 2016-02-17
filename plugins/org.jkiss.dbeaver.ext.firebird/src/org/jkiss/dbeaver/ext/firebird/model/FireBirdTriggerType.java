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
