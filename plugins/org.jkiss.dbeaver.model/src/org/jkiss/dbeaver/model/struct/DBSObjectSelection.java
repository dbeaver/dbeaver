/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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

import org.jkiss.code.NotNull;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Lazy checkbox selection over a database-object hierarchy. Reading or changing selection never loads metadata.
 */
public class DBSObjectSelection {
    private final Set<DBSObject> selected = new LinkedHashSet<>();
    private final Map<DBSObject, Integer> descendantCounts = new HashMap<>();
    private final Function<DBSObject, DBSObject> parent;
    private final Function<DBSObject, List<DBSObject>> cachedChildren;

    public DBSObjectSelection(
        @NotNull Function<DBSObject, DBSObject> parent,
        @NotNull Function<DBSObject, List<DBSObject>> cachedChildren
    ) {
        this.parent = parent;
        this.cachedChildren = cachedChildren;
    }

    public void clear() {
        selected.clear();
        descendantCounts.clear();
    }

    public void setChecked(@NotNull DBSObject object, boolean checked) {
        if (checked && isFullyChecked(object)) {
            return;
        }
        selected.stream().filter(child -> isDescendant(child, object)).toList().forEach(this::remove);
        if (checked) {
            add(object);
        } else {
            remove(object);
            for (DBSObject ancestor = parent.apply(object); ancestor != null; ancestor = parent.apply(ancestor)) {
                if (remove(ancestor)) {
                    // A displayed child has cached ancestor paths; preserve its siblings without loading any others.
                    for (DBSObject child = object; child != ancestor; child = parent.apply(child)) {
                        DBSObject container = parent.apply(child);
                        for (DBSObject sibling : cachedChildren.apply(container)) {
                            if (sibling != child) {
                                add(sibling);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    public boolean isChecked(@NotNull DBSObject object) {
        return isFullyChecked(object) || descendantCounts.containsKey(object);
    }

    public boolean isGrayed(@NotNull DBSObject object) {
        return descendantCounts.containsKey(object) && !isSubtreeFullyChecked(object);
    }

    @NotNull
    public List<DBSObject> getSelectedObjects() {
        return List.copyOf(selected);
    }

    private void add(@NotNull DBSObject object) {
        if (selected.add(object)) {
            for (DBSObject ancestor = parent.apply(object); ancestor != null; ancestor = parent.apply(ancestor)) {
                descendantCounts.merge(ancestor, 1, Integer::sum);
            }
        }
    }

    private boolean remove(@NotNull DBSObject object) {
        if (!selected.remove(object)) {
            return false;
        }
        for (DBSObject ancestor = parent.apply(object); ancestor != null; ancestor = parent.apply(ancestor)) {
            descendantCounts.computeIfPresent(ancestor, (key, count) -> count == 1 ? null : count - 1);
        }
        return true;
    }

    private boolean isSubtreeFullyChecked(@NotNull DBSObject object) {
        if (isFullyChecked(object)) {
            return true;
        }
        if (!descendantCounts.containsKey(object)) {
            return false;
        }
        List<DBSObject> children = cachedChildren.apply(object);
        return !children.isEmpty() && children.stream().allMatch(this::isSubtreeFullyChecked);
    }

    private boolean isFullyChecked(@NotNull DBSObject object) {
        for (DBSObject current = object; current != null; current = parent.apply(current)) {
            if (selected.contains(current)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDescendant(@NotNull DBSObject object, @NotNull DBSObject ancestor) {
        for (DBSObject current = parent.apply(object); current != null; current = parent.apply(current)) {
            if (current == ancestor) {
                return true;
            }
        }
        return false;
    }
}
