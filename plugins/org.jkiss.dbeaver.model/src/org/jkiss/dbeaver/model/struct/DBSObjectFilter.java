/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.struct;

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

    private transient List<Pattern> includePatterns = null;
    private transient List<Pattern> excludePatterns = null;

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
            this.include = filter.include == null ? null : new ArrayList<String>(filter.include);
            this.exclude = filter.exclude == null ? null : new ArrayList<String>(filter.exclude);
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
            include = new ArrayList<String>();
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
            exclude = new ArrayList<String>();
        }
        exclude.add(name);
        this.excludePatterns = null;
    }

    public void setExclude(List<String> exclude)
    {
        this.exclude = exclude;
        this.excludePatterns = null;
    }

    public boolean isEmpty()
    {
        return !enabled || (CommonUtils.isEmpty(include) && CommonUtils.isEmpty(exclude));
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
            includePatterns = new ArrayList<Pattern>(include.size());
            for (String inc : include) {
                if (!inc.isEmpty()) {
                    includePatterns.add(Pattern.compile(
                        SQLUtils.makeLikePattern(inc), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
                }
            }
        }
        if (includePatterns != null) {
            // Match includes (at least one should match)
            boolean matched = false;
            for (Pattern pattern : includePatterns) {
                if (pattern.matcher(name).matches()) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
        }

        if (excludePatterns == null && !CommonUtils.isEmpty(exclude)) {
            excludePatterns = new ArrayList<Pattern>(exclude.size());
            for (String exc : exclude) {
                if (!exc.isEmpty()) {
                    excludePatterns.add(Pattern.compile(
                        SQLUtils.makeLikePattern(exc), Pattern.CASE_INSENSITIVE | Pattern.MULTILINE));
                }
            }
        }
        if (excludePatterns != null) {
            // Match excludes
            for (Pattern pattern : excludePatterns) {
                if (pattern.matcher(name).matches()) {
                    return false;
                }
            }
        }
        // Done
        return true;
    }

}
