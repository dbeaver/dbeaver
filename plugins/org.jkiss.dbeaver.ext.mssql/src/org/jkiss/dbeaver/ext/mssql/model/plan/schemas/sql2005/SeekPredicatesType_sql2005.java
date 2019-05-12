
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SeekPredicatesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SeekPredicatesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="SeekPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SeekPredicatesType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicate"
})
public class SeekPredicatesType_sql2005 {

    @XmlElement(name = "SeekPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<SeekPredicateType_sql2005> seekPredicate;

    /**
     * Gets the value of the seekPredicate property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seekPredicate property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSeekPredicate().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SeekPredicateType_sql2005 }
     * 
     * 
     */
    public List<SeekPredicateType_sql2005> getSeekPredicate() {
        if (seekPredicate == null) {
            seekPredicate = new ArrayList<SeekPredicateType_sql2005>();
        }
        return this.seekPredicate;
    }

}
