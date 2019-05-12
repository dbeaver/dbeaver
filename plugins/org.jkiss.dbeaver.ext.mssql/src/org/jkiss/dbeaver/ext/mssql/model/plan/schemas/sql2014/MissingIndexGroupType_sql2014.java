
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MissingIndexGroupType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MissingIndexGroupType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MissingIndex" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MissingIndexType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Impact" use="required" type="{http://www.w3.org/2001/XMLSchema}double" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MissingIndexGroupType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "missingIndex"
})
public class MissingIndexGroupType_sql2014 {

    @XmlElement(name = "MissingIndex", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<MissingIndexType_sql2014> missingIndex;
    @XmlAttribute(name = "Impact", required = true)
    protected double impact;

    /**
     * Gets the value of the missingIndex property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the missingIndex property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMissingIndex().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MissingIndexType_sql2014 }
     * 
     * 
     */
    public List<MissingIndexType_sql2014> getMissingIndex() {
        if (missingIndex == null) {
            missingIndex = new ArrayList<MissingIndexType_sql2014>();
        }
        return this.missingIndex;
    }

    /**
     * Gets the value of the impact property.
     * 
     */
    public double getImpact() {
        return impact;
    }

    /**
     * Sets the value of the impact property.
     * 
     */
    public void setImpact(double value) {
        this.impact = value;
    }

}
