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
package org.jkiss.dbeaver.ui.controls.decorations;

import org.jkiss.dbeaver.model.DBIcon;

final class SnowflakeIcons {
    static final DBIcon FLAKE_1 = new DBIcon("flake1", "misc/decorations/flake1.png");
    static final DBIcon FLAKE_2 = new DBIcon("flake2", "misc/decorations/flake2.png");
    static final DBIcon FLAKE_3 = new DBIcon("flake3", "misc/decorations/flake3.png");
    static final DBIcon FLAKE_4 = new DBIcon("flake4", "misc/decorations/flake4.png");
    static final DBIcon FLAKE_5 = new DBIcon("flake5", "misc/decorations/flake5.png");
    static final DBIcon FLAKE_6 = new DBIcon("flake6", "misc/decorations/flake6.png");
    static final DBIcon FLAKE_7 = new DBIcon("flake7", "misc/decorations/flake7.png");
    static final DBIcon FLAKE_8 = new DBIcon("flake8", "misc/decorations/flake8.png");
    static final DBIcon FLAKE_9 = new DBIcon("flake9", "misc/decorations/flake9.png");
    static final DBIcon FLAKE_10 = new DBIcon("flake10", "misc/decorations/flake10.png");
    static final DBIcon FLAKE_11 = new DBIcon("flake11", "misc/decorations/flake11.png");
    static final DBIcon FLAKE_12 = new DBIcon("flake12", "misc/decorations/flake12.png");

    static {
        DBIcon.loadIcons(SnowflakeIcons.class);
    }

    private SnowflakeIcons() {
    }
}
