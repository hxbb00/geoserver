//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference
// Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2018.12.17 at 04:13:52 PM PST
//

package org.geoserver.mapml.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;

/**
 * Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="type" use="required" type="{}inputType" /&gt;
 *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="rel" type="{}inputRelType" /&gt;
 *       &lt;attribute name="list" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *       &lt;attribute name="position" type="{}positionType" /&gt;
 *       &lt;attribute name="axis" type="{}axisType" /&gt;
 *       &lt;attribute name="units" type="{}unitType" /&gt;
 *       &lt;attribute name="required" type="{http://www.w3.org/2001/XMLSchema}boolean" /&gt;
 *       &lt;attribute name="min" type="{http://www.w3.org/2001/XMLSchema}decimal" /&gt;
 *       &lt;attribute name="max" type="{http://www.w3.org/2001/XMLSchema}decimal" /&gt;
 *       &lt;attribute name="step" type="{http://www.w3.org/2001/XMLSchema}anySimpleType" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "")
@XmlRootElement(name = "map-input", namespace = "http://www.w3.org/1999/xhtml")
public class Input {

    @XmlAttribute(name = "name", required = true)
    @XmlSchemaType(name = "anySimpleType")
    protected String name;

    @XmlAttribute(name = "type", required = true)
    protected InputType type;

    @XmlAttribute(name = "value")
    @XmlSchemaType(name = "anySimpleType")
    protected String value;

    @XmlAttribute(name = "rel")
    protected InputRelType rel;

    @XmlAttribute(name = "list")
    @XmlSchemaType(name = "anySimpleType")
    protected String list;

    @XmlAttribute(name = "position")
    protected PositionType position;

    @XmlAttribute(name = "axis")
    protected AxisType axis;

    @XmlAttribute(name = "units")
    protected UnitType units;

    @XmlAttribute(name = "required")
    protected Boolean required;

    @XmlAttribute(name = "min")
    protected String min;

    @XmlAttribute(name = "max")
    protected String max;

    @XmlAttribute(name = "step")
    @XmlSchemaType(name = "anySimpleType")
    protected String step;

    /**
     * Gets the value of the name property.
     *
     * @return possible object is {@link String }
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is {@link String }
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the type property.
     *
     * @return possible object is {@link InputType }
     */
    public InputType getType() {
        return type;
    }

    /**
     * Sets the value of the type property.
     *
     * @param value allowed object is {@link InputType }
     */
    public void setType(InputType value) {
        this.type = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is {@link String }
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the value of the rel property.
     *
     * @return possible object is {@link InputRelType }
     */
    public InputRelType getRel() {
        return rel;
    }

    /**
     * Sets the value of the rel property.
     *
     * @param value allowed object is {@link InputRelType }
     */
    public void setRel(InputRelType value) {
        this.rel = value;
    }

    /**
     * Gets the value of the list property.
     *
     * @return possible object is {@link String }
     */
    public String getList() {
        return list;
    }

    /**
     * Sets the value of the list property.
     *
     * @param value allowed object is {@link String }
     */
    public void setList(String value) {
        this.list = value;
    }

    /**
     * Gets the value of the position property.
     *
     * @return possible object is {@link PositionType }
     */
    public PositionType getPosition() {
        return position;
    }

    /**
     * Sets the value of the position property.
     *
     * @param value allowed object is {@link PositionType }
     */
    public void setPosition(PositionType value) {
        this.position = value;
    }

    /**
     * Gets the value of the axis property.
     *
     * @return possible object is {@link AxisType }
     */
    public AxisType getAxis() {
        return axis;
    }

    /**
     * Sets the value of the axis property.
     *
     * @param value allowed object is {@link AxisType }
     */
    public void setAxis(AxisType value) {
        this.axis = value;
    }

    /**
     * Gets the value of the units property.
     *
     * @return possible object is {@link UnitType }
     */
    public UnitType getUnits() {
        return units;
    }

    /**
     * Sets the value of the units property.
     *
     * @param value allowed object is {@link UnitType }
     */
    public void setUnits(UnitType value) {
        this.units = value;
    }

    /**
     * Gets the value of the required property.
     *
     * @return possible object is {@link Boolean }
     */
    public Boolean isRequired() {
        return required;
    }

    /**
     * Sets the value of the required property.
     *
     * @param value allowed object is {@link Boolean }
     */
    public void setRequired(Boolean value) {
        this.required = value;
    }

    /**
     * Gets the value of the min property.
     *
     * @return the value of the min property
     */
    public String getMin() {
        return min;
    }

    /** Sets the value of the min property. */
    public void setMin(String value) {
        this.min = value;
    }

    /** Gets the value of the max property. */
    public String getMax() {
        return max;
    }

    /**
     * Sets the value of the max property.
     *
     * @param value allowed object is {@link double }
     */
    public void setMax(String value) {
        this.max = value;
    }

    /**
     * Gets the value of the step property.
     *
     * @return possible object is {@link String }
     */
    public String getStep() {
        return step;
    }

    /**
     * Sets the value of the step property.
     *
     * @param value allowed object is {@link String }
     */
    public void setStep(String value) {
        this.step = value;
    }
}
