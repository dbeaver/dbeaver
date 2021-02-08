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

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;

/**
 * Object state
 */
public class DBSObjectState
{
    public static final DBSObjectState NORMAL = new DBSObjectState("Normal", null);
    public static final DBSObjectState INVALID = new DBSObjectState("Invalid", DBIcon.OVER_ERROR);
    public static final DBSObjectState ACTIVE = new DBSObjectState("Active", DBIcon.OVER_SUCCESS);
    public static final DBSObjectState UNKNOWN = new DBSObjectState("Unknown", DBIcon.OVER_UNKNOWN);

    private final String title;
    private final DBPImage overlayImage;

    public DBSObjectState(String title, DBPImage overlayImage)
    {
        this.title = title;
        this.overlayImage = overlayImage;
    }

    public String getTitle()
    {
        return title;
    }

    public DBPImage getOverlayImage()
    {
        return overlayImage;
    }

}