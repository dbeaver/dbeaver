
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 			Information on parallel thread usage.
 * 			Branches: Attribute. total number of concurrent branches of query plan.  
 * 				Query would need additional worker threads of at least (Branches)* (Degree of Parallelism)
 * 			UsedThreads: Attribute maximum number of used parallel threads.  This is available only for statistics XML
 * 			Then follows a list of one or more ThreadReservation elements.
 * 			
 * 
 * <p>Java class for ThreadStatType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ThreadStatType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ThreadReservation" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ThreadReservationType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Branches" use="required" type="{http://www.w3.org/2001/XMLSchema}int" />
 *       &lt;attribute name="UsedThreads" type="{http://www.w3.org/2001/XMLSchema}int" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ThreadStatType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "threadReservation"
})
public class ThreadStatType_sql2014sp2 {

    @XmlElement(name = "ThreadReservation", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ThreadReservationType_sql2014sp2> threadReservation;
    @XmlAttribute(name = "Branches", required = true)
    protected int branches;
    @XmlAttribute(name = "UsedThreads")
    protected Integer usedThreads;

    /**
     * Gets the value of the threadReservation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the threadReservation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getThreadReservation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ThreadReservationType_sql2014sp2 }
     * 
     * 
     */
    public List<ThreadReservationType_sql2014sp2> getThreadReservation() {
        if (threadReservation == null) {
            threadReservation = new ArrayList<ThreadReservationType_sql2014sp2>();
        }
        return this.threadReservation;
    }

    /**
     * Gets the value of the branches property.
     * 
     */
    public int getBranches() {
        return branches;
    }

    /**
     * Sets the value of the branches property.
     * 
     */
    public void setBranches(int value) {
        this.branches = value;
    }

    /**
     * Gets the value of the usedThreads property.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getUsedThreads() {
        return usedThreads;
    }

    /**
     * Sets the value of the usedThreads property.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setUsedThreads(Integer value) {
        this.usedThreads = value;
    }

}
