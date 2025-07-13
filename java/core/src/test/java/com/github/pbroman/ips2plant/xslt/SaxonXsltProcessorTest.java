package com.github.pbroman.ips2plant.xslt;


import org.junit.jupiter.api.BeforeEach;

public class SaxonXsltProcessorTest extends AbstractXsltProcessorTest{

    @BeforeEach
    public void setUp() {
        processor = new SaxonXsltProcessor();
    }

}
