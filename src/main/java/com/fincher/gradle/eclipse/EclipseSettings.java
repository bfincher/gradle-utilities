package com.fincher.gradle.eclipse;

import java.io.IOException;
import java.io.UncheckedIOException;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

public class EclipseSettings implements Plugin<Project> {
    public void apply(Project project) {
        PluginContainer plugins = project.getPlugins();
        plugins.withType(JavaPlugin.class, __ -> { // NOSONAR
            plugins.apply(EclipsePlugin.class);
            project.getExtensions().getByType(EclipseModel.class).getJdt()
                    .file(merger -> merger.withProperties(properties -> {
                        try {
                            properties.load(
                                    EclipseSettings.class.getClassLoader()
                                            .getResourceAsStream("default.jdt.core.prefs"));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }));
            project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME,
                    task -> task.dependsOn(EclipsePlugin.ECLIPSE_JDT_TASK_NAME));
        });
    }
}
