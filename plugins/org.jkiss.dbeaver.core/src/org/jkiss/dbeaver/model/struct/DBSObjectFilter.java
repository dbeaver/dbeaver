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
    private boolean enabled;
    private List<String> includes;
    private List<String> excludes;

    public DBSObjectFilter()
    {
    }

    public DBSObjectFilter(DBSObjectFilter filter)
    {
        this.name = filter.name;
        this.description = filter.description;
        this.enabled = filter.enabled;
        this.includes = new ArrayList<String>(filter.includes);
        this.excludes = new ArrayList<String>(filter.excludes);
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

    public List<String> getIncludes()
    {
        return includes;
    }

    public void setIncludes(List<String> includes)
    {
        this.includes = includes;
    }

    public List<String> getExcludes()
    {
        return excludes;
    }

    public void setExcludes(List<String> excludes)
    {
        this.excludes = excludes;
    }
}
