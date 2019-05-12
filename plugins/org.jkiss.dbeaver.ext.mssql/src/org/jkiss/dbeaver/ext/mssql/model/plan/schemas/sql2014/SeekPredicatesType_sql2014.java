
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

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
 *       &lt;choice>
 *         &lt;element name="SeekPredicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateType" maxOccurs="unbounded"/>
 *         &lt;element name="SeekPredicateNew" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicateNewType" maxOccurs="unbounded"/>
 *       &lt;/choice>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SeekPredicatesType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicate",
    "seekPredicateNew"
})
public class SeekPredicatesType_sql2014 {

    @XmlElement(name = "SeekPredicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<SeekPredicateType_sql2014> seekPredicate;
    @XmlElement(name = "SeekPredicateNew", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<SeekPredicateNewType_sql2014> seekPredicateNew;

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
     * {@link SeekPredicateType_sql2014 }
     * 
     * 
     */
    public List<SeekPredicateType_sql2014> getSeekPredicate() {
        if (seekPredicate == null) {
            seekPredicate = new ArrayList<SeekPredicateType_sql2014>();
        }
        return this.seekPredicate;
    }

    /**
     * Gets the value of the seekPredicateNew property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the seekPredicateNew property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSeekPredicateNew().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link SeekPredicateNewType_sql2014 }
     * 
     * 
     */
    public List<SeekPredicateNewType_sql2014> getSeekPredicateNew() {
        if (seekPredicateNew == null) {
            seekPredicateNew = new ArrayList<SeekPredicateNewType_sql2014>();
        }
        return this.seekPredicateNew;
    }

}
