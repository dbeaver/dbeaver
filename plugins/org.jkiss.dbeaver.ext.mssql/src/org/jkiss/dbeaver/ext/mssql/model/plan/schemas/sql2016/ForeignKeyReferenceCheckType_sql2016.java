
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for ForeignKeyReferenceCheckType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="ForeignKeyReferenceCheckType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="IndexScan" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}IndexScanType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ForeignKeyReferenceCheckType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "indexScan"
})
public class ForeignKeyReferenceCheckType_sql2016 {

    @XmlElement(name = "IndexScan", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected IndexScanType_sql2016 indexScan;

    /**
     * Gets the value of the indexScan property.
     * 
     * @return
     *     possible object is
     *     {@link IndexScanType_sql2016 }
     *     
     */
    public IndexScanType_sql2016 getIndexScan() {
        return indexScan;
    }

    /**
     * Sets the value of the indexScan property.
     * 
     * @param value
     *     allowed object is
     *     {@link IndexScanType_sql2016 }
     *     
     */
    public void setIndexScan(IndexScanType_sql2016 value) {
        this.indexScan = value;
    }

}
