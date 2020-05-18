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
package org.jkiss.dbeaver.ui.gis.panel;

import java.util.ArrayList;
import java.util.List;

/**
 * Database select dialog
 */
public class GISEditorUtils {

    private static final int MAX_RECENT_SRID_SIZE = 10;

    private static final List<Integer> recentSRIDs = new ArrayList<>();

    public static List<Integer> getRecentSRIDs() {
        return recentSRIDs;
    }

    public static void addRecentSRID(int srid) {
        if (!recentSRIDs.contains(srid)) {
            recentSRIDs.add(srid);
        }
    }

    public static void curRecentSRIDs() {
        if (recentSRIDs.size() > MAX_RECENT_SRID_SIZE) {
            recentSRIDs.remove(0);
        }
    }

}