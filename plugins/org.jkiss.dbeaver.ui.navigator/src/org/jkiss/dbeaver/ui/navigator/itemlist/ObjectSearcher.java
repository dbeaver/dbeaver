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
package org.jkiss.dbeaver.ui.navigator.itemlist;

import org.jkiss.dbeaver.model.DBPNamedObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.ui.ISearchExecutor;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public abstract class ObjectSearcher<OBJECT_TYPE extends DBPObject> implements ISearchExecutor {

    private Pattern curSearchPattern;
    private int curSearchIndex;
    private Set<OBJECT_TYPE> curSearchResult = null;

    @Override
    public boolean performSearch(String searchString, int options)
    {
        boolean caseSensitiveSearch = (options & SEARCH_CASE_SENSITIVE) != 0;
        if (!CommonUtils.isEmpty(searchString) && curSearchPattern == null || !CommonUtils.equalObjects(curSearchPattern.pattern(), SQLUtils.makeLikePattern(searchString))) {
            try {
                curSearchPattern = Pattern.compile(SQLUtils.makeLikePattern(searchString), caseSensitiveSearch ? 0 : Pattern.CASE_INSENSITIVE);
            } catch (PatternSyntaxException e) {
                setInfo(e.getMessage());
                return false;
            }
            curSearchIndex = -1;
            Set<OBJECT_TYPE> oldSearchResult = curSearchResult;
            curSearchResult = null;
            boolean found = false;
            Collection<OBJECT_TYPE> nodes = getContent();
            if (!CommonUtils.isEmpty(nodes)) {
                for (OBJECT_TYPE node : nodes) {
                    if (matchesSearch(node)) {
                        if (curSearchResult == null) {
                            curSearchResult = new LinkedHashSet<>(50);
                        }
                        curSearchResult.add(node);
                        updateObject(node);
                        if (!found) {
                            curSearchIndex++;
                            selectObject(node);
                            revealObject(node);
                        }
                        found = true;
                    }
                }
            }
            if (!CommonUtils.isEmpty(oldSearchResult)) {
                for (OBJECT_TYPE oldNode : oldSearchResult) {
                    if (curSearchResult == null || !curSearchResult.contains(oldNode)) {
                        updateObject(oldNode);
                    }
                }
            }
            return found;
        } else {
            boolean findNext = ((options & SEARCH_NEXT) != 0);
            boolean findPrev = ((options & SEARCH_PREVIOUS) != 0);
            if ((findNext || findPrev) && !CommonUtils.isEmpty(curSearchResult)) {
                if (findNext) {
                    curSearchIndex++;
                    if (curSearchIndex >= curSearchResult.size()) {
                        curSearchIndex = 0;
                    }
                } else {
                    curSearchIndex--;
                    if (curSearchIndex < 0) {
                        curSearchIndex = curSearchResult.size() - 1;
                    }
                }
                int index = 0;
                for (OBJECT_TYPE node : curSearchResult) {
                    if (index++ == curSearchIndex) {
                        selectObject(node);
                        revealObject(node);
                        break;
                    }
                }
            }
            return !CommonUtils.isEmpty(curSearchResult);
        }
    }

    @Override
    public void cancelSearch()
    {
        if (curSearchPattern != null) {
            curSearchPattern = null;
            curSearchIndex = 0;
            if (curSearchResult != null) {
                Set<OBJECT_TYPE> oldSearchResult = curSearchResult;
                curSearchResult = null;
                for (OBJECT_TYPE oldNode : oldSearchResult) {
                    updateObject(oldNode);
                }
                selectObject(null);
            }
        }
    }

    protected boolean matchesSearch(OBJECT_TYPE element)
    {
        if (curSearchPattern == null) {
            return false;
        }
        if (element instanceof DBPNamedObject) {
            return curSearchPattern.matcher(((DBPNamedObject)element).getName()).find();
        } else {
            return false;
        }
    }

    protected Pattern getSearchPattern() {
        return curSearchPattern;
    }

    public boolean hasObject(OBJECT_TYPE object)
    {
        return curSearchResult != null && curSearchResult.contains(object);
    }

    protected abstract void setInfo(String message);

    protected abstract Collection<OBJECT_TYPE> getContent();

    protected abstract void selectObject(OBJECT_TYPE object);

    protected abstract void updateObject(OBJECT_TYPE object);

    protected abstract void revealObject(OBJECT_TYPE object);

}
