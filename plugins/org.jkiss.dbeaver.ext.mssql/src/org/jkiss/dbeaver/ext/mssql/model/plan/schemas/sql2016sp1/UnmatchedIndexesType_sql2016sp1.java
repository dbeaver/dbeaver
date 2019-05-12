
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UnmatchedIndexesType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="UnmatchedIndexesType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Parameterization" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ParameterizationType"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnmatchedIndexesType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "parameterization"
})
public class UnmatchedIndexesType_sql2016sp1 {

    @XmlElement(name = "Parameterization", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ParameterizationType_sql2016sp1 parameterization;

    /**
     * Gets the value of the parameterization property.
     * 
     * @return
     *     possible object is
     *     {@link ParameterizationType_sql2016sp1 }
     *     
     */
    public ParameterizationType_sql2016sp1 getParameterization() {
        return parameterization;
    }

    /**
     * Sets the value of the parameterization property.
     * 
     * @param value
     *     allowed object is
     *     {@link ParameterizationType_sql2016sp1 }
     *     
     */
    public void setParameterization(ParameterizationType_sql2016sp1 value) {
        this.parameterization = value;
    }

}
