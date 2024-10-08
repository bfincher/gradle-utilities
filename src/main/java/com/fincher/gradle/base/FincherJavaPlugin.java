package com.fincher.gradle.base;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.UnknownTaskException;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.external.javadoc.CoreJavadocOptions;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.gradle.spotless.SpotlessPlugin;
import com.diffplug.spotless.LineEnding;
import com.fincher.gradle.checkstyle.CheckstyleConfigPlugin;
import com.fincher.gradle.eclipse.EclipseSettings;
import com.fincher.sonarlint.SonarlintExtension;
import com.fincher.sonarlint.SonarlintPlugin;

public class FincherJavaPlugin implements Plugin<Project> {

    public void apply(Project project) throws GradleException {
        project.getPluginManager().apply(FincherBasePlugin.class);
        project.getPluginManager().apply(CheckstyleConfigPlugin.class);
        configureJava(project);
        configureJavadoc(project);
        configureEclipse(project);
        configureSonarlint(project);
        configurePublishing(project);

        try {
            configureSpotless(project);
        } catch (IOException e) {
            throw new GradleException(e.getMessage(), e);
        }
    }

    private void configureJava(Project project) {
        project.getPluginManager().apply(JacocoPlugin.class);
        project.getPluginManager().apply(JavaLibraryPlugin.class);

        JavaPluginExtension javaExtension = project.getExtensions().getByType(JavaPluginExtension.class);
        javaExtension.withJavadocJar();
        javaExtension.withSourcesJar();

        project.getTasks().named("test", Test.class).configure(Test::useJUnitPlatform);
    }

    private void configureSonarlint(Project project) {
        project.getPluginManager().apply(SonarlintPlugin.class);

        SonarlintExtension extension = project.getExtensions().getByType(SonarlintExtension.class);
        extension.getReports().create("html").getEnabled().set(true);
        extension.getExcludeRules().add("java:S1181");
        extension.getExcludeRules().add("java:S2166");

        disableTaskIfPresent(project, "sonarlintTest");
        disableTaskIfPresent(project, "sonarlintFunctionalTest");
    }

    private boolean disableTaskIfPresent(Project project, String taskName) {
        try {
            project.getTasks().named(taskName).configure(task -> task.setEnabled(false));
            return true;
        } catch (UnknownTaskException e) {
            return false;
        }
    }

    private void configureJavadoc(Project project) {
        project.getTasks().withType(Javadoc.class, task -> {
            if (task.getOptions() instanceof CoreJavadocOptions) {
                ((CoreJavadocOptions)task.getOptions()).addStringOption("Xdoclint:none", "-quiet");
            }
        });
    }

    private void configurePublishing(Project project) {
        PublishingExtension pe = project.getExtensions().getByType(PublishingExtension.class);

        pe.getPublications().create("main", MavenPublication.class,
                publication -> publication.from(project.getComponents().findByName("java")));
    }

    private void configureEclipse(Project project) {
        project.getPluginManager().apply(EclipseSettings.class);
        EclipseClasspath classpath = project.getExtensions().getByType(EclipseModel.class).getClasspath();
        classpath.setDownloadJavadoc(true);
        classpath.setDownloadSources(true);
    }

    private void configureSpotless(Project project) throws IOException {
        project.getPluginManager().apply(SpotlessPlugin.class);

        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path formatterConfig = tmpDir.resolve("eclipse-formatter.xml");
        Path importOrderConfig = tmpDir.resolve("eclipse.importorder");

        CheckstyleConfigPlugin.copyFileFromClasspathIfChanged("eclipse-formatter.xml", formatterConfig);
        CheckstyleConfigPlugin.copyFileFromClasspathIfChanged("eclipse.importorder", importOrderConfig);

        SpotlessExtension extension = project.getExtensions().getByType(SpotlessExtension.class);
        extension.java(action -> {
            action.importOrderFile(importOrderConfig);
            action.removeUnusedImports();
            action.eclipse().configFile(formatterConfig);
            action.setLineEndings(LineEnding.UNIX);
        });
    }
}
