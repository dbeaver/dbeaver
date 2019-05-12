
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 *  A list of query wait statistics. 
 * 
 * <p>Java class for WaitStatListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="WaitStatListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Wait" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}WaitStatType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "WaitStatListType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "wait"
})
public class WaitStatListType_sql2016sp1 {

    @XmlElement(name = "Wait", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<WaitStatType_sql2016sp1> wait;

    /**
     * Gets the value of the wait property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the wait property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWait().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link WaitStatType_sql2016sp1 }
     * 
     * 
     */
    public List<WaitStatType_sql2016sp1> getWait() {
        if (wait == null) {
            wait = new ArrayList<WaitStatType_sql2016sp1>();
        }
        return this.wait;
    }

}
