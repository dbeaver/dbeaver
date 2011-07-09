/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data.query;

import java.util.List;

/**
 * Criteria
 */
public class DBQCriterionGroup implements DBQCriterion {

    private final List<DBQCriterion> criteria;
    private boolean conjunction;

    public DBQCriterionGroup(List<DBQCriterion> criteria, boolean conjunction)
    {
        this.criteria = criteria;
        this.conjunction = conjunction;
    }

    public List<DBQCriterion> getCriteria()
    {
        return criteria;
    }

    public void addCriterion(DBQCriterion criterion)
    {
        criteria.add(criterion);
    }

    public boolean removeCriterion(DBQCriterion criterion)
    {
        return criteria.remove(criterion);
    }

    public boolean isConjunction()
    {
        return conjunction;
    }

    public void setConjunction(boolean conjunction)
    {
        this.conjunction = conjunction;
    }
}