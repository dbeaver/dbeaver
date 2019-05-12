
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2014;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for IdentType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="IdentType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="Table" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IdentType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnReference"
})
public class IdentType_sql2014 {

    @XmlElement(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ColumnReferenceType_sql2014 columnReference;
    @XmlAttribute(name = "Table")
    protected String table;

    /**
     * Gets the value of the columnReference property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceType_sql2014 }
     *     
     */
    public ColumnReferenceType_sql2014 getColumnReference() {
        return columnReference;
    }

    /**
     * Sets the value of the columnReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceType_sql2014 }
     *     
     */
    public void setColumnReference(ColumnReferenceType_sql2014 value) {
        this.columnReference = value;
    }

    /**
     * Gets the value of the table property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the value of the table property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTable(String value) {
        this.table = value;
    }

}
