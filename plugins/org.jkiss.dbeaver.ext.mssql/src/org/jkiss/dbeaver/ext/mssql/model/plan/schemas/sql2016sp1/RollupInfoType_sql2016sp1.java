
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * Additional information about a rollup. The highest level is the number of group by columns.
 * 
 * <p>Java class for RollupInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RollupInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="RollupLevel" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RollupLevelType" maxOccurs="unbounded" minOccurs="2"/>
 *       &lt;/sequence>
 *       &lt;attribute name="HighestLevel" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RollupInfoType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "rollupLevel"
})
public class RollupInfoType_sql2016sp1 {

    @XmlElement(name = "RollupLevel", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<RollupLevelType_sql2016sp1> rollupLevel;
    @XmlAttribute(name = "HighestLevel", required = true)
    protected int highestLevel;

    /**
     * Gets the value of the rollupLevel property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the rollupLevel property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRollupLevel().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link RollupLevelType_sql2016sp1 }
     * 
     * 
     */
    public List<RollupLevelType_sql2016sp1> getRollupLevel() {
        if (rollupLevel == null) {
            rollupLevel = new ArrayList<RollupLevelType_sql2016sp1>();
        }
        return this.rollupLevel;
    }

    /**
     * Gets the value of the highestLevel property.
     * 
     */
    public int getHighestLevel() {
        return highestLevel;
    }

    /**
     * Sets the value of the highestLevel property.
     * 
     */
    public void setHighestLevel(int value) {
        this.highestLevel = value;
    }

}
