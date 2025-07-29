package com.github.pbroman.ips2plant.core;

import java.io.File;
import java.util.Objects;

public abstract class AbstractTest {

    protected File getResourceFile(String resourceName) {
        var resource = getClass().getClassLoader().getResource(resourceName);
        return new File(Objects.requireNonNull(resource, "No resource with name %s found".formatted(resourceName)).getFile());
    }
}
