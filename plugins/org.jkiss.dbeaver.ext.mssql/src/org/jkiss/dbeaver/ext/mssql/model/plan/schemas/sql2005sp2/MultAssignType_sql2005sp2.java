
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for MultAssignType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="MultAssignType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Assign" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}AssignType" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "MultAssignType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "assign"
})
public class MultAssignType_sql2005sp2 {

    @XmlElement(name = "Assign", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected List<AssignType_sql2005sp2> assign;

    /**
     * Gets the value of the assign property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the assign property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAssign().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AssignType_sql2005sp2 }
     * 
     * 
     */
    public List<AssignType_sql2005sp2> getAssign() {
        if (assign == null) {
            assign = new ArrayList<AssignType_sql2005sp2>();
        }
        return this.assign;
    }

}
