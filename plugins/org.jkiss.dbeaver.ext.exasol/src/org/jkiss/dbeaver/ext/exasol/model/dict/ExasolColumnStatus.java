/**
 * 
 */
package org.jkiss.dbeaver.ext.exasol.model.dict;

import org.jkiss.code.NotNull;

/**
 * @author Karl Griesser
 *
 */
public enum ExasolColumnStatus {
	
	OUTDATED("Column definition is outdated");

    private String name;

    // -----------------
    // Constructor
    // -----------------
    private ExasolColumnStatus(String name)
    {
        this.name = name;
    }

    // -----------------------
    // Display @Property Value
    // -----------------------
    @Override
    public String toString()
    {
        return name;
    }

    // ----------------
    // Standard Getters
    // ----------------
    @NotNull
    public String getName()
    {
        return name;
    }
	

}
