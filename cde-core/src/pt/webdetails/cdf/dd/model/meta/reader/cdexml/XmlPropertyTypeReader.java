/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package pt.webdetails.cdf.dd.model.meta.reader.cdexml;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;
import pt.webdetails.cdf.dd.model.core.Thing;
import pt.webdetails.cdf.dd.model.meta.PropertyType;
import pt.webdetails.cdf.dd.model.core.reader.IThingReadContext;
import pt.webdetails.cdf.dd.model.core.reader.IThingReader;
import pt.webdetails.cdf.dd.model.core.reader.ThingReadException;
import pt.webdetails.cdf.dd.util.Utils;

/**
 * @author dcleao
 */
public class XmlPropertyTypeReader implements IThingReader
{
  protected static final Log _logger = LogFactory.getLog(XmlPropertyTypeReader.class);

  public PropertyType.Builder read(
          IThingReadContext context,
          java.lang.Object source,
          String sourcePath)
          throws ThingReadException
  {
    PropertyType.Builder builder = new PropertyType.Builder();
    read(builder, context, (Element)source, sourcePath);
    return builder;
  }

  public void read(
          Thing.Builder builder,
          IThingReadContext context,
          java.lang.Object source,
          String sourcePath)
          throws ThingReadException
  {
    read((PropertyType.Builder)builder, context, (Element)source, sourcePath);
  }

  public void read(
          PropertyType.Builder builder,
          IThingReadContext context,
          Element propertyElem,
          String sourcePath)
          throws ThingReadException
  {
    String name = Utils.getNodeText("Header/Name", propertyElem, "");

    builder
      .setSourcePath(sourcePath)
      .setName(name)
      .setLabel(Utils.getNodeText("Header/Description", propertyElem))
      .setTooltip(Utils.getNodeText("Header/Tooltip", propertyElem));

    String advanced = Utils.getNodeText("Header/Advanced", propertyElem);
    if("true".equalsIgnoreCase(advanced))
    {
      builder.setCategory(PropertyType.CAT_ADVANCED);
      builder.setCategoryLabel(PropertyType.CAT_ADVANCED_DESC);
    }

    builder
      .setBase(Utils.getNodeText("Header/Parent", propertyElem))
      .setDefaultValue(Utils.getNodeText("Header/DefaultValue", propertyElem))
      .setVersion(Utils.getNodeText("Header/Version", propertyElem));
    
    String visibleText = Utils.getNodeText("Header/Visible", propertyElem);
    if(StringUtils.isNotEmpty(visibleText))
    {
      builder.setVisible("true".equalsIgnoreCase(visibleText));
    }
    
    String valueTypeText = Utils.getNodeText("Header/OutputType", propertyElem);
    if(StringUtils.isNotEmpty(valueTypeText))
    {
      try
      {
        builder.setValueType(PropertyType.ValueType.valueOf(valueTypeText.toUpperCase()));
      }
      catch(IllegalArgumentException ex) // Enum conversion
      {
        _logger.warn(
            "PropertyType '" + name + "' in file '" + sourcePath + "' has an invalid 'OutputType' value: '" + valueTypeText + "'.",
            ex);
      }
    }

    String order = Utils.getNodeText("Header/Order", propertyElem);
    if(StringUtils.isNotEmpty(order))
    {
      try {
        int orderInt = Integer.parseInt(order);
        if(orderInt < 0) { orderInt = 0; }

        builder.setOrder(orderInt);
      } catch(NumberFormatException ex) {
        _logger.warn(
           "PropertyType '" + name + "' in file '" + sourcePath + "' has an invalid 'Order' value: '" + order + "'.",
           ex);
      }
    }

    // -----------
    
    String rendererKind = Utils.getNodeText("Header/InputType/@type", propertyElem);

    if(StringUtils.isEmpty(rendererKind) || "custom".equalsIgnoreCase(rendererKind))
    {
      builder.setInputType(Utils.getNodeText("Header/InputType", propertyElem));
    }
    else if("valuelist"  .equalsIgnoreCase(rendererKind) ||
            "dynamiclist".equalsIgnoreCase(rendererKind))
    {
      // Let the default InputType be determined.
      
      // A custom renderer will also be created with name: «name + "CustomRenderer"»
      // It will be _based_ on the following class:
      String baseRendererType = Utils.getNodeText("Header/InputType/@base", propertyElem);
      if(StringUtils.isEmpty(baseRendererType)) {
        baseRendererType = "SelectRenderer";
      }

      // This seems too much UI detail... Add as an attribute.
      builder.addAttribute("BaseRenderer", baseRendererType);
    }
    else
    {
      throw new ThingReadException(
        "PropertyType '" + name + "' in file '" + sourcePath + "' has an invalid 'Header/InputType/@type' value: '" + rendererKind + "'.");
    }

    builder.setPossibleValuesSource(Utils.getNodeText("Values/@source", propertyElem));

    List<Element> labeledValueElems = propertyElem.selectNodes("Values/Value");
    for (Element labeledValueElem : labeledValueElems)
    {
      builder.addPossibleValue(
    		  Utils.getNodeText(".", labeledValueElem),
    		  Utils.getNodeText("@display", labeledValueElem));
    }
  }
}