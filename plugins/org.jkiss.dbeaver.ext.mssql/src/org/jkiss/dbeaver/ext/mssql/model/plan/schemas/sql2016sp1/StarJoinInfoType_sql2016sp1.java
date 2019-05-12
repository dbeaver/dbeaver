
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2016sp1;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Additional information about Star Join structure.
 * 
 * <p>Java class for StarJoinInfoType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="StarJoinInfoType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="Root" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *       &lt;attribute name="OperationType" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *             &lt;enumeration value="Fetch"/>
 *             &lt;enumeration value="Index Intersection"/>
 *             &lt;enumeration value="Index Filter"/>
 *             &lt;enumeration value="Index Lookup"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "StarJoinInfoType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class StarJoinInfoType_sql2016sp1 {

    @XmlAttribute(name = "Root")
    protected Boolean root;
    @XmlAttribute(name = "OperationType", required = true)
    protected String operationType;

    /**
     * Gets the value of the root property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean getRoot() {
        return root;
    }

    /**
     * Sets the value of the root property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setRoot(Boolean value) {
        this.root = value;
    }

    /**
     * Gets the value of the operationType property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * Sets the value of the operationType property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOperationType(String value) {
        this.operationType = value;
    }

}
