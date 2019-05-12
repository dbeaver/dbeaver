
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2012;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for SetPredicateElementType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="SetPredicateElementType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType">
 *       &lt;attribute name="SetPredicateType" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}SetPredicateType" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SetPredicateElementType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
public class SetPredicateElementType_sql2012
    extends ScalarExpressionType_sql2012
{

    @XmlAttribute(name = "SetPredicateType")
    protected SetPredicateType_sql2012 setPredicateType;

    /**
     * Gets the value of the setPredicateType property.
     * 
     * @return
     *     possible object is
     *     {@link SetPredicateType_sql2012 }
     *     
     */
    public SetPredicateType_sql2012 getSetPredicateType() {
        return setPredicateType;
    }

    /**
     * Sets the value of the setPredicateType property.
     * 
     * @param value
     *     allowed object is
     *     {@link SetPredicateType_sql2012 }
     *     
     */
    public void setSetPredicateType(SetPredicateType_sql2012 value) {
        this.setPredicateType = value;
    }

}
