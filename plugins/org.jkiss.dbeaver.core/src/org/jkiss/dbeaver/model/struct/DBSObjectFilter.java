/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import java.util.ArrayList;
import java.util.List;

/**
 * Object filter configuration
 */
public class DBSObjectFilter
{
    private String name;
    private String description;
    private boolean enabled = true;
    private List<String> include = new ArrayList<String>();
    private List<String> exclude = new ArrayList<String>();

    public DBSObjectFilter()
    {
        include.add("");
        exclude.add("");
    }

    public DBSObjectFilter(DBSObjectFilter filter)
    {
        this.name = filter.name;
        this.description = filter.description;
        this.enabled = filter.enabled;
        this.include = filter.include == null ? null : new ArrayList<String>(filter.include);
        this.exclude = filter.exclude == null ? null : new ArrayList<String>(filter.exclude);
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

    public void setInclude(List<String> include)
    {
        this.include = include;
    }

    public List<String> getExclude()
    {
        return exclude;
    }

    public void setExclude(List<String> exclude)
    {
        this.exclude = exclude;
    }
}
