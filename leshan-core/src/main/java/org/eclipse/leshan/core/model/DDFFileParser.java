/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 * 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * 
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 * 
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.core.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.leshan.core.model.ResourceModel.Operations;
import org.eclipse.leshan.core.model.ResourceModel.Type;
import org.eclipse.leshan.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A parser for Object DDF files.
 */
public class DDFFileParser {

    private static final Logger LOG = LoggerFactory.getLogger(DDFFileParser.class);

    private final DocumentBuilderFactory factory;

    public DDFFileParser() {
        factory = DocumentBuilderFactory.newInstance();
    }

    public List<ObjectModel> parse(File ddfFile) {
        InputStream input = null;
        try {
            input = new FileInputStream(ddfFile);
            return parse(input, ddfFile.getName());
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + ddfFile.getName(), e);
        }
        return Collections.emptyList();
    }

    public List<ObjectModel> parse(InputStream inputStream, String streamName) {
        streamName = streamName == null ? "" : streamName;

        LOG.debug("Parsing DDF file {}", streamName);

        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(inputStream);

            ArrayList<ObjectModel> objects = new ArrayList<ObjectModel>();
            NodeList nodeList = document.getDocumentElement().getElementsByTagName("Object");
            for (int i = 0; i < nodeList.getLength(); i++) {
                objects.add(parseObject(nodeList.item(i)));
            }
            return objects;
        } catch (Exception e) {
            LOG.error("Could not parse the resource definition file " + streamName, e);
        }
        return Collections.emptyList();
    }

    private ObjectModel parseObject(Node object) {

        Integer id = null;
        String name = null;
        String description = null;
        String version = ObjectModel.DEFAULT_VERSION;
        boolean multiple = false;
        boolean mandatory = false;
        List<ResourceModel> resources = new ArrayList<ResourceModel>();

        for (int i = 0; i < object.getChildNodes().getLength(); i++) {
            Node field = object.getChildNodes().item(i);
            if (field.getNodeName().equals("ObjectID")) {
                id = Integer.valueOf(field.getTextContent());
            } else if (field.getNodeName().equals("Name")) {
                name = field.getTextContent();
            } else if (field.getNodeName().equals("Description1")) {
                description = field.getTextContent();
            } else if (field.getNodeName().equals("ObjectVersion")) {
                if (!StringUtils.isEmpty(field.getTextContent()))
                    version = field.getTextContent();
            } else if (field.getNodeName().equals("MultipleInstances")) {
                multiple = "Multiple".equals(field.getTextContent());
            } else if (field.getNodeName().equals("Mandatory")) {
                mandatory = "Mandatory".equals(field.getTextContent());
            } else if (field.getNodeName().equals("Resources")) {
                for (int j = 0; j < field.getChildNodes().getLength(); j++) {
                    Node item = field.getChildNodes().item(j);
                    if (item.getNodeName().equals("Item")) {
                        resources.add(this.parseResource(item));
                    }
                }
            }
        }

        return new ObjectModel(id, name, description, version, multiple, mandatory, resources);

    }

    private ResourceModel parseResource(Node item) {

        Integer id = Integer.valueOf(item.getAttributes().getNamedItem("ID").getTextContent());
        String name = null;
        Operations operations = Operations.NONE;
        boolean multiple = false;
        boolean mandatory = false;
        Type type = Type.STRING;
        String rangeEnumeration = null;
        String units = null;
        String description = null;

        for (int i = 0; i < item.getChildNodes().getLength(); i++) {
            Node field = item.getChildNodes().item(i);
            if (field.getNodeName().equals("Name")) {
                name = field.getTextContent();
            } else if (field.getNodeName().equals("Operations")) {
                String strOp = field.getTextContent();
                if (strOp != null && !strOp.isEmpty()) {
                    operations = Operations.valueOf(strOp);
                }
            } else if (field.getNodeName().equals("MultipleInstances")) {
                multiple = "Multiple".equals(field.getTextContent());
            } else if (field.getNodeName().equals("Mandatory")) {
                mandatory = "Mandatory".equals(field.getTextContent());
            } else if (field.getNodeName().equals("Type")) {
                if (field.getTextContent().equals("String")) {
                    type = Type.STRING;
                } else if (field.getTextContent().equals("Integer")) {
                    type = Type.INTEGER;
                } else if (field.getTextContent().equals("Float")) {
                    type = Type.FLOAT;
                } else if (field.getTextContent().equals("Boolean")) {
                    type = Type.BOOLEAN;
                } else if (field.getTextContent().equals("Opaque")) {
                    type = Type.OPAQUE;
                } else if (field.getTextContent().equals("Time")) {
                    type = Type.TIME;
                } else if (field.getTextContent().equals("Objlnk")) {
                    type = Type.OBJLNK;
                }
            } else if (field.getNodeName().equals("RangeEnumeration")) {
                rangeEnumeration = field.getTextContent();
            } else if (field.getNodeName().equals("Units")) {
                units = field.getTextContent();
            } else if (field.getNodeName().equals("Description")) {
                description = field.getTextContent();
            }

        }

        return new ResourceModel(id, name, operations, multiple, mandatory, type, rangeEnumeration, units, description);
    }

}
