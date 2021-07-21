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

package org.jkiss.dbeaver.model.data;

import org.jkiss.code.NotNull;

/**
 * Label value pair with count
 */
public class DBDLabelValuePairExt extends DBDLabelValuePair {

    private long count;

    public DBDLabelValuePairExt(String label, Object value, long count) {
        super(label, value);
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    @Override
    public int compareTo(@NotNull Object o) {
        if (o instanceof DBDLabelValuePairExt) {
            long countCmp = ((DBDLabelValuePairExt) o).count - count;
            if (countCmp != 0) {
                return (int) countCmp;
            }
        }
        return super.compareTo(o);
    }

    public void incCount() {
        this.count++;
    }
}
