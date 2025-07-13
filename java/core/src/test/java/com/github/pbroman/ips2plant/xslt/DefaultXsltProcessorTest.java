package com.github.pbroman.ips2plant.xslt;


import org.junit.jupiter.api.BeforeEach;

public class DefaultXsltProcessorTest extends AbstractXsltProcessorTest {

    @BeforeEach
    public void setUp() {
        processor = new DefaultXsltProcessor();
    }
}
