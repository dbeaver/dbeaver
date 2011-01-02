/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data.criteria;

import java.util.List;

/**
 * Criteria
 */
public class DBDCriterionGroup implements DBDCriterion {

    private final List<DBDCriterion> criteria;
    private boolean conjunction;

    public DBDCriterionGroup(List<DBDCriterion> criteria, boolean conjunction)
    {
        this.criteria = criteria;
        this.conjunction = conjunction;
    }

    public List<DBDCriterion> getCriteria()
    {
        return criteria;
    }

    public void addCriterion(DBDCriterion criterion)
    {
        criteria.add(criterion);
    }

    public boolean removeCriterion(DBDCriterion criterion)
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