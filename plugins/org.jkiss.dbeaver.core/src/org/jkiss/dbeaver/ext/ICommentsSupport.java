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

package org.jkiss.dbeaver.ext;

import org.jkiss.code.Nullable;
import org.jkiss.utils.Pair;

/**
 * Comments manager.
 * <p/>
 * Contains information about comments
 */
public interface ICommentsSupport {
    /**
     * Two-item array containing begin and end of multi-line comments.
     * @return string array or null if multi-line comments are not supported
     */
    @Nullable
    Pair<String, String> getMultiLineComments();

    /**
     * List of possible single-line comment prefixes
     * @return comment prefixes or null if single line comments are nto supported
     */
    String[] getSingleLineComments();
}
