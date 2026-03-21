package com.github.pbroman.ips2plant.core;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.Serializer;
import net.sf.saxon.s9api.XdmAtomicValue;

public class XsltProcessor {

    private static final String XSL_RESOURCE = "/xsl/ips2plant.xsl";

    public String transform(String collectionXml, Map<String, String> params) {
        try {
            var processor = new Processor(false);

            var xmlSource = processor.newDocumentBuilder()
                    .build(new StreamSource(new StringReader(collectionXml)));

            var xslUrl = getClass().getResource(XSL_RESOURCE);
            if (xslUrl == null) {
                throw new IllegalStateException("XSLT resource not found: " + XSL_RESOURCE);
            }

            var xsltCompiler = processor.newXsltCompiler();
            params.forEach((key, value) ->
                    xsltCompiler.setParameter(new QName(key), new XdmAtomicValue(value)));

            var executable = xsltCompiler.compile(new StreamSource(xslUrl.toExternalForm()));
            var xsltTransformer = executable.load();

            var writer = new StringWriter();
            var destination = processor.newSerializer(writer);
            destination.setOutputProperty(Serializer.Property.METHOD, "text");

            xsltTransformer.setInitialContextNode(xmlSource);
            xsltTransformer.setDestination(destination);
            xsltTransformer.transform();

            return writer.toString();

        } catch (SaxonApiException e) {
            throw new RuntimeException("XSLT transformation failed", e);
        }
    }
}