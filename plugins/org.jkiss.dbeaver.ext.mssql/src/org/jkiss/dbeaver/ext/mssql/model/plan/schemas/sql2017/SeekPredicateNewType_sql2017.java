
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SeekPredicateNewType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SeekPredicateNewType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SeekKeys" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateType" maxOccurs="2"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SeekPredicateNewType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekKeys"
})
public class SeekPredicateNewType_sql2017 {

    @XmlElement(name = "SeekKeys", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<SeekPredicateType_sql2017> seekKeys;

    /**
     * Gets the value of the seekKeys property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seekKeys property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSeekKeys().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SeekPredicateType_sql2017 }
     * 
     * 
     */
    public List<SeekPredicateType_sql2017> getSeekKeys() {
        if (seekKeys == null) {
            seekKeys = new ArrayList<SeekPredicateType_sql2017>();
        }
        return this.seekKeys;
    }

}
