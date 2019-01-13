/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Object filter configuration
 */
public class DBSObjectFilter
{
    private String name;
    private String description;
    private boolean enabled = true;
    private List<String> include;
    private List<String> exclude;

    private transient List<Object> includePatterns = null;
    private transient List<Object> excludePatterns = null;

    public DBSObjectFilter()
    {
    }

    public DBSObjectFilter(String includeString, @Nullable String excludeString)
    {
        if (include != null) {
            this.include = SQLUtils.splitFilter(includeString);
        }
        if (exclude != null) {
            this.exclude = SQLUtils.splitFilter(excludeString);
        }
    }

    public DBSObjectFilter(DBSObjectFilter filter)
    {
        if (filter != null) {
            this.name = filter.name;
            this.description = filter.description;
            this.enabled = filter.enabled;
            this.include = filter.include == null ? null : new ArrayList<>(filter.include);
            this.exclude = filter.exclude == null ? null : new ArrayList<>(filter.exclude);
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public List<String> getInclude()
    {
        return include;
    }

    public void addInclude(String name)
    {
        if (include == null) {
            include = new ArrayList<>();
        }
        include.add(name);
        this.includePatterns = null;
    }

    public void setInclude(List<String> include)
    {
        this.include = include;
        this.includePatterns = null;
    }

    public List<String> getExclude()
    {
        return exclude;
    }

    public void addExclude(String name)
    {
        if (exclude == null) {
            exclude = new ArrayList<>();
        }
        exclude.add(name);
        this.excludePatterns = null;
    }

    public void setExclude(List<String> exclude)
    {
        this.exclude = exclude;
        this.excludePatterns = null;
    }

    public boolean isNotApplicable()
    {
        return !enabled || isEmpty();
    }

    public boolean isEmpty()
    {
        return CommonUtils.isEmpty(include) && CommonUtils.isEmpty(exclude);
    }

    public boolean hasSingleMask()
    {
        return include != null && include.size() == 1 && CommonUtils.isEmpty(exclude);
    }

    @Nullable
    public String getSingleMask()
    {
        return !CommonUtils.isEmpty(include) ? include.get(0) : null;
    }
    
    public synchronized boolean matches(String name)
    {
        if (includePatterns == null && !CommonUtils.isEmpty(include)) {
            includePatterns = new ArrayList<>(include.size());
            for (String inc : include) {
                if (!inc.isEmpty()) {
                    includePatterns.add(makePattern(inc));
                }
            }
        }
        if (includePatterns != null) {
            // Match includes (at least one should match)
            boolean matched = false;
            for (Object pattern : includePatterns) {
                if (matchesPattern(pattern, name)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        if (excludePatterns == null && !CommonUtils.isEmpty(exclude)) {
            excludePatterns = new ArrayList<>(exclude.size());
            for (String exc : exclude) {
                if (!exc.isEmpty()) {
                    excludePatterns.add(makePattern(exc));
                }
            }
        }
        if (excludePatterns != null) {
            // Match excludes
            for (Object pattern : excludePatterns) {
                if (matchesPattern(pattern, name)) {
                    return false;
                }
            }
        }
        // Done
        return true;
    }

    private static boolean matchesPattern(Object pattern, String name) {
        if (pattern instanceof Pattern) {
            return ((Pattern)pattern).matcher(name).matches();
        } else {
            return ((String)pattern).equalsIgnoreCase(name);
        }
    }

    @NotNull
    private static Object makePattern(String str) {
        if (SQLUtils.isLikePattern(str)) {
            return Pattern.compile(
                SQLUtils.makeLikePattern(str), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        } else {
            return str;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DBSObjectFilter)) {
            return false;
        }
        DBSObjectFilter source = (DBSObjectFilter)obj;

        return CommonUtils.equalObjects(name, source.name) &&
            CommonUtils.equalObjects(description, source.description) &&
            enabled == source.enabled &&
            CommonUtils.equalObjects(include, source.include) &&
            CommonUtils.equalObjects(exclude, source.exclude);
    }

    @Override
    public int hashCode() {
        return CommonUtils.hashCode(name) +
                CommonUtils.hashCode(description) +
                (enabled ? 1 : 0) +
                CommonUtils.hashCode(include) +
                CommonUtils.hashCode(exclude);
    }
}
