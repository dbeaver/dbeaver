/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.editors.sql;

/**
 * Nested SQL editor contributor.
 * It is really nested if more than one contributor exists at the same time.
 * Otherwise it should act as normal contributor
 */
public class SQLEditorContributorNested extends SQLEditorContributor {

    private static int nestedCount = 0;

    private int nestedId;

    public SQLEditorContributorNested()
    {
        nestedId = nestedCount;
        nestedCount++;
    }

    @Override
    public void dispose()
    {
        super.dispose();
        nestedCount--;
    }

    @Override
    protected boolean isNestedEditor()
    {
        return true;//nestedId > 0;
    }

}
