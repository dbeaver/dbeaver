
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MissingIndexesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MissingIndexesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="MissingIndexGroup" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}MissingIndexGroupType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MissingIndexesType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "missingIndexGroup"
})
public class MissingIndexesType_sql2016 {

    @XmlElement(name = "MissingIndexGroup", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<MissingIndexGroupType_sql2016> missingIndexGroup;

    /**
     * Gets the value of the missingIndexGroup property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the missingIndexGroup property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getMissingIndexGroup().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link MissingIndexGroupType_sql2016 }
     * 
     * 
     */
    public List<MissingIndexGroupType_sql2016> getMissingIndexGroup() {
        if (missingIndexGroup == null) {
            missingIndexGroup = new ArrayList<MissingIndexGroupType_sql2016>();
        }
        return this.missingIndexGroup;
    }

}
