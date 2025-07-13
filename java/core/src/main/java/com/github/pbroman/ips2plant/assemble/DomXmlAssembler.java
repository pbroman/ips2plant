package com.github.pbroman.ips2plant.assemble;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.github.pbroman.ips2plant.api.XmlAssembler;

public class DomXmlAssembler implements XmlAssembler {

    @Override
    public void assemble(Map<String, File> ipsFiles, Path destination) {

        try {
            var domBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var collection = assemble(domBuilder, ipsFiles);
            writeToDestination(collection, destination);

        } catch (ParserConfigurationException | IOException | SAXException | TransformerException e) {
            // TODO
            throw new RuntimeException(e);
        }
    }


    private Document assemble(DocumentBuilder builder, Map<String, File> ipsFiles) throws IOException, SAXException {

        // Create collection
        var collectionDoc = builder.newDocument();
        var collectionRoot = collectionDoc.createElement("collection");
        collectionDoc.appendChild(collectionRoot);

        // Add all files
        for (var entry : ipsFiles.entrySet()) {
            var ipsDoc = builder.parse(entry.getValue());
            var ipsRoot = ipsDoc.getDocumentElement();
            ipsRoot.setAttribute("className", entry.getKey());
            ipsRoot.removeAttribute("xmlns");
            ipsRoot.removeAttribute("xmlns:xsi");
            ipsRoot.removeAttribute("xsi:schemaLocation");

            collectionRoot.appendChild(collectionDoc.adoptNode(ipsRoot.cloneNode(true)));
        }
        return collectionDoc;
    }

    void writeToDestination(Document collection, Path destination) throws TransformerException {
        var dom = new DOMSource(collection);
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StreamResult result = new StreamResult(destination.toFile());
        transformer.transform(dom, result);
    }

}
