package com.github.pbroman.ips2plant.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

import com.github.pbroman.ips2plant.cli.command.Ips2PlantCommand;
import com.github.pbroman.ips2plant.cli.config.Ips2PlantConfiguration;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

@SpringBootApplication
@Import(Ips2PlantConfiguration.class)
public class Ips2PlantApplication implements CommandLineRunner , ExitCodeGenerator {

    private final IFactory factory;
    private final Ips2PlantCommand command;
    private int exitCode;

    public Ips2PlantApplication(IFactory factory, Ips2PlantCommand command) {
        this.factory = factory;
        this.command = command;
    }


    @Override
    public void run(String... args) throws Exception {
        exitCode = new CommandLine(command, factory).execute(args);
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(Ips2PlantApplication.class, args)));
    }
}
