package org.jkiss.dbeaver.model.struct;

/**
 * Dictionary descriptor
 */
public class DBSDictionary {

    private String entityReference;
    private String name;
    private String descriptionColumns;

    public DBSDictionary(String entityReference, String name, String descriptionColumns) {
        this.entityReference = entityReference;
        this.name = name;
        this.descriptionColumns = descriptionColumns;
    }

    public DBSDictionary(DBSDictionary dictionary)
    {
        this.entityReference = dictionary.entityReference;
        this.name = dictionary.name;
        this.descriptionColumns = dictionary.descriptionColumns;
    }

    public String getEntityReference()
    {
        return entityReference;
    }

    public String getName() {
        return name;
    }

    public String getDescriptionColumns() {
        return descriptionColumns;
    }

}
