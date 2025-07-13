package com.github.pbroman.ips2plant.cli.config;

import org.springframework.context.annotation.Bean;

import com.github.pbroman.ips2plant.assemble.DomXmlAssembler;
import com.github.pbroman.ips2plant.cli.command.Ips2PlantCommand;
import com.github.pbroman.ips2plant.collect.DefaultIpsFileCollector;
import com.github.pbroman.ips2plant.kyverno.Ips2PlantRunner;
import com.github.pbroman.ips2plant.xslt.SaxonXsltProcessor;

public class Ips2PlantConfiguration {

    @Bean
    Ips2PlantRunner ips2PlantRunner() {
        return new Ips2PlantRunner(
                new DefaultIpsFileCollector(),
                new DomXmlAssembler(),
                new SaxonXsltProcessor());
    }

    @Bean
    Ips2PlantCommand command(Ips2PlantRunner runner) {
        return new Ips2PlantCommand(runner);
    }

}
