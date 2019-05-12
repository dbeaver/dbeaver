
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SingleColumnReferenceType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SingleColumnReferenceType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ColumnReference" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ColumnReferenceType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SingleColumnReferenceType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "columnReference"
})
public class SingleColumnReferenceType_sql2016 {

    @XmlElement(name = "ColumnReference", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ColumnReferenceType_sql2016 columnReference;

    /**
     * Gets the value of the columnReference property.
     * 
     * @return
     *     possible object is
     *     {@link ColumnReferenceType_sql2016 }
     *     
     */
    public ColumnReferenceType_sql2016 getColumnReference() {
        return columnReference;
    }

    /**
     * Sets the value of the columnReference property.
     * 
     * @param value
     *     allowed object is
     *     {@link ColumnReferenceType_sql2016 }
     *     
     */
    public void setColumnReference(ColumnReferenceType_sql2016 value) {
        this.columnReference = value;
    }

}
