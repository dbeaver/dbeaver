
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ColumnReferenceListType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ColumnReferenceListType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ColumnReferenceListType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnReference"
})
public class ColumnReferenceListType_sql2014 {

    @XmlElement(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected List<ColumnReferenceType_sql2014> columnReference;

    /**
     * Gets the value of the columnReference property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the columnReference property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getColumnReference().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link ColumnReferenceType_sql2014 }
     * 
     * 
     */
    public List<ColumnReferenceType_sql2014> getColumnReference() {
        if (columnReference == null) {
            columnReference = new ArrayList<ColumnReferenceType_sql2014>();
        }
        return this.columnReference;
    }

}
