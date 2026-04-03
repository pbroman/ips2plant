package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlAssembler {

    public String assemble(Map<String, File> ipsFiles) {
        return assemble(ipsFiles, Map.of());
    }

    public String assemble(Map<String, File> ipsFiles, Map<String, String> mavenModules) {
        try {
            var domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

            var collectionDoc = domBuilder.newDocument();
            var collectionRoot = collectionDoc.createElement("collection");
            collectionDoc.appendChild(collectionRoot);

            for (var entry : ipsFiles.entrySet()) {
                var ipsDoc = domBuilder.parse(entry.getValue());
                var ipsRoot = ipsDoc.getDocumentElement();
                ipsRoot.setAttribute("className", entry.getKey());
                var mavenModule = mavenModules.get(entry.getKey());
                if (mavenModule != null) {
                    ipsRoot.setAttribute("mavenModule", mavenModule);
                }
                ipsRoot.removeAttribute("xmlns");
                ipsRoot.removeAttribute("xmlns:xsi");
                ipsRoot.removeAttribute("xsi:schemaLocation");

                collectionRoot.appendChild(collectionDoc.adoptNode(ipsRoot.cloneNode(true)));
            }

            return serialize(collectionDoc);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            throw new RuntimeException("Failed to assemble IPS files into collection XML", e);
        }
    }

    private String serialize(Document doc) throws TransformerException {
        var transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        var writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }
}