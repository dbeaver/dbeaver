
package org.jkiss.dbeaver.ext.mssql.model.plan.schemas.sql2017;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 * 				Typical user defined table valued function doesn't have a relational child element. If a relational child
 * 				is present then the operator is a special internal table valued function that hosts native code.
 * 			
 * 
 * <p>Java class for TableValuedFunctionType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="TableValuedFunctionType">
 *   &lt;complexContent>
 *     &lt;extension base="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpBaseType">
 *       &lt;sequence>
 *         &lt;element name="Object" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ObjectType" minOccurs="0"/>
 *         &lt;element name="Predicate" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionType" minOccurs="0"/>
 *         &lt;element name="RelOp" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}RelOpType" minOccurs="0"/>
 *         &lt;element name="ParameterList" type="{http://schemas.microsoft.com/sqlserver/2004/07/showplan}ScalarExpressionListType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "TableValuedFunctionType", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan", propOrder = {
    "object",
    "predicate",
    "relOp",
    "parameterList"
})
public class TableValuedFunctionType_sql2017
    extends RelOpBaseType_sql2017
{

    @XmlElement(name = "Object", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ObjectType_sql2017 object;
    @XmlElement(name = "Predicate", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionType_sql2017 predicate;
    @XmlElement(name = "RelOp", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected RelOpType_sql2017 relOp;
    @XmlElement(name = "ParameterList", namespace = "http://schemas.microsoft.com/sqlserver/2004/07/showplan")
    protected ScalarExpressionListType_sql2017 parameterList;

    /**
     * Gets the value of the object property.
     * 
     * @return
     *     possible object is
     *     {@link ObjectType_sql2017 }
     *     
     */
    public ObjectType_sql2017 getObject() {
        return object;
    }

    /**
     * Sets the value of the object property.
     * 
     * @param value
     *     allowed object is
     *     {@link ObjectType_sql2017 }
     *     
     */
    public void setObject(ObjectType_sql2017 value) {
        this.object = value;
    }

    /**
     * Gets the value of the predicate property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public ScalarExpressionType_sql2017 getPredicate() {
        return predicate;
    }

    /**
     * Sets the value of the predicate property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionType_sql2017 }
     *     
     */
    public void setPredicate(ScalarExpressionType_sql2017 value) {
        this.predicate = value;
    }

    /**
     * Gets the value of the relOp property.
     * 
     * @return
     *     possible object is
     *     {@link RelOpType_sql2017 }
     *     
     */
    public RelOpType_sql2017 getRelOp() {
        return relOp;
    }

    /**
     * Sets the value of the relOp property.
     * 
     * @param value
     *     allowed object is
     *     {@link RelOpType_sql2017 }
     *     
     */
    public void setRelOp(RelOpType_sql2017 value) {
        this.relOp = value;
    }

    /**
     * Gets the value of the parameterList property.
     * 
     * @return
     *     possible object is
     *     {@link ScalarExpressionListType_sql2017 }
     *     
     */
    public ScalarExpressionListType_sql2017 getParameterList() {
        return parameterList;
    }

    /**
     * Sets the value of the parameterList property.
     * 
     * @param value
     *     allowed object is
     *     {@link ScalarExpressionListType_sql2017 }
     *     
     */
    public void setParameterList(ScalarExpressionListType_sql2017 value) {
        this.parameterList = value;
    }

}
