
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2005sp2;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for FilterType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="FilterType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType"/>
 *       &lt;/sequence>
 *       &lt;attribute name="StartupExpression" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "FilterType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "relOp",
    "predicate"
})
public class FilterType_sql2005sp2
    extends RelOpBaseType_sql2005sp2
{

    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected RelOpType_sql2005sp2 relOp;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", required = true)
    protected ScalarExpressionType_sql2005sp2 predicate;
    @XmlAttribute(name = "StartupExpression", required = true)
    protected boolean startupExpression;

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2005sp2 }
     *     
     */
    public RelOpType_sql2005sp2 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2005sp2 }
     *     
     */
    public void setRelOp(RelOpType_sql2005sp2 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2005sp2 }
     *     
     */
    public ScalarExpressionType_sql2005sp2 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2005sp2 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2005sp2 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the startupExpression property.
     * 
     */
    public boolean isStartupExpression() {
        return startupExpression;
    }

    /**
     * Sets the value of the startupExpression property.
     * 
     */
    public void setStartupExpression(boolean value) {
        this.startupExpression = value;
    }

}
