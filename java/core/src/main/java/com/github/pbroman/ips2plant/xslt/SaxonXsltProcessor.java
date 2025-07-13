package com.github.pbroman.ips2plant.xslt;

import java.nio.file.Path;
import java.util.Map;

import com.github.pbroman.ips2plant.api.XsltProcessor;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;

public class SaxonXsltProcessor implements XsltProcessor {

    @Override
    public void process(Path xsl, Path xml, Path result, Map<String, String> stringParams) {
        try {
            var processor = new Processor(false);
            var xmlSource = processor.newDocumentBuilder().build(xml.toFile());
            var destination = processor.newSerializer(result.toFile());

            var xsltCompiler = processor.newXsltCompiler();
            stringParams.forEach((key, value) ->
                    xsltCompiler.setParameter(new QName(key), new XdmAtomicValue(value))
            );
            var xsltTransformer = xsltCompiler.compile(xsl.toFile()).load();

            xsltTransformer.setInitialContextNode(xmlSource);
            xsltTransformer.setDestination(destination);
            xsltTransformer.transform();

        } catch (SaxonApiException e) {
            throw new RuntimeException(e);
        }
    }
}
