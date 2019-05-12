
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for RemoteRangeType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="RemoteRangeType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RemoteType">
 *       &lt;sequence>
 *         &lt;element name="SeekPredicates" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SeekPredicatesType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RemoteRangeType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "seekPredicates"
})
public class RemoteRangeType_sql2017
    extends RemoteType_sql2017
{

    @XmlElement(name = "SeekPredicates", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected SeekPredicatesType_sql2017 seekPredicates;

    /**
     * Gets the value of the seekPredicates property.
     * 
     * @return
     *     possible object is
     *     {@link SeekPredicatesType_sql2017 }
     *     
     */
    public SeekPredicatesType_sql2017 getSeekPredicates() {
        return seekPredicates;
    }

    /**
     * Sets the value of the seekPredicates property.
     * 
     * @param value
     *     allowed object is
     *     {@link SeekPredicatesType_sql2017 }
     *     
     */
    public void setSeekPredicates(SeekPredicatesType_sql2017 value) {
        this.seekPredicates = value;
    }

}
