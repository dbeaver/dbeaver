package org.jkiss.dbeaver.ext.generic.model;

public interface GenericCheckConstraint {
    String getCheckConstraintExpression();

    void setCheckConstraintExpression(String expression);
}
