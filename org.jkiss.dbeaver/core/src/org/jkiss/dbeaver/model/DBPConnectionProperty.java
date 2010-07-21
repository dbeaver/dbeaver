package org.jkiss.dbeaver.model;

/**
 * DBPConnectionProperty
 */
public interface DBPConnectionProperty {

    String getName();

    String getDescription();

    boolean isRequired();

    String getValue();

    String[] getChoices();

}
