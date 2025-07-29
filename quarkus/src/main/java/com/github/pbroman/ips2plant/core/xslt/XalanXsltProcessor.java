package com.github.pbroman.ips2plant.core.xslt;

import java.nio.file.Path;
import java.util.Map;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.xalan.xsltc.trax.SmartTransformerFactoryImpl;

import com.github.pbroman.ips2plant.core.api.XsltProcessor;

public class XalanXsltProcessor implements XsltProcessor {

    @Override
    public void process(Path xsl, Path xml, Path result, Map<String, String> stringParams) {
        try {
            var xslSource = new StreamSource(xsl.toFile());
            var xmlSource = new StreamSource(xml.toFile());
            var destination = new StreamResult(result.toFile());
            var transformer = SmartTransformerFactoryImpl.newInstance()
                    .newTemplates(xslSource)
                    .newTransformer();
            stringParams.forEach(transformer::setParameter);
            transformer.transform(xmlSource, destination);

        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }
}
