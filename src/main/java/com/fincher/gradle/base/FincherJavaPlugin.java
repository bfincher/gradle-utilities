package com.fincher.gradle.base;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiFunction;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Transformer;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;

import com.diffplug.gradle.spotless.SpotlessExtension;
import com.diffplug.spotless.LineEnding;
import com.fincher.gradle.checkstyle.CheckstyleConfigPlugin;
import com.fincher.gradle.eclipse.EclipseSettings;

public class FincherJavaPlugin implements Plugin<Project> {

    public void apply(Project project) throws GradleException {
        project.getPluginManager().apply(FincherBasePlugin.class);
        project.getPluginManager().apply(CheckstyleConfigPlugin.class);
        configureJava(project);
        configureEclipse(project);
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

        project.getTasks().named("test", Test.class).configure(task -> task.useJUnitPlatform());
    }

    private void configurePublishing(Project project) {
        PublishingExtension pe = project.getExtensions().getByType(PublishingExtension.class);

        pe.getPublications().create("main", MavenPublication.class, publication -> {
            publication.from(project.getComponents().findByName("java"));
        });
    }

    private void configureEclipse(Project project) {
        project.getPluginManager().apply(EclipseSettings.class);
        EclipseClasspath classpath = project.getExtensions().getByType(EclipseModel.class).getClasspath();
        classpath.setDownloadJavadoc(true);
        classpath.setDownloadSources(true);
    }

    private void configureSpotless(Project project) throws IOException {

        Path configDir = new File(project.getBuildDir(), "generated").toPath();
        Path formatterConfig = configDir.resolve("eclipse-formatter.xml");
        Path importOrderConfig = configDir.resolve("eclipse.importorder");

        SpotlessExtension extension = project.getExtensions().getByType(SpotlessExtension.class);
        extension.java(action -> {
            action.importOrderFile(new FileProvider("eclipse.importorder", importOrderConfig));
            action.removeUnusedImports();
            action.eclipse().configFile(new FileProvider("eclipse-formatter.xml", formatterConfig));
            action.setLineEndings(LineEnding.UNIX);
        });
    }

    private static class FileProvider implements Provider<File> {

        private final Path destFile;
        private final String source;

        FileProvider(String source, Path destFile) {
            this.source = source;
            this.destFile = destFile;
        }

        @Override
        public File get() throws GradleException {
            try {
                Path parentDir = destFile.getParent();
                if (!Files.exists(parentDir)) {
                    Files.createDirectories(parentDir);
                }

                CheckstyleConfigPlugin.copyFileFromClasspathIfChanged(source, destFile);

                return destFile.toFile();
            } catch (IOException e) {
                throw new GradleException(e.getMessage(), e);
            }
        }

        @Override
        public File getOrNull() {
            return get();
        }

        @Override
        public File getOrElse(File defaultValue) {
            return get();
        }

        @Override
        public <S> Provider<S> map(Transformer<? extends S, ? super File> transformer) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public <S> Provider<S> flatMap(Transformer<? extends Provider<? extends S>, ? super File> transformer) {
            throw new RuntimeException("Not implemented");
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public Provider<File> orElse(File value) {
            return this;
        }

        @Override
        public Provider<File> orElse(Provider<? extends File> provider) {
            return this;
        }

        @Override
        public Provider<File> forUseAtConfigurationTime() {
            return this;
        }

        @Override
        public <U, R> Provider<R> zip(Provider<U> right, BiFunction<? super File, ? super U, ? extends R> combiner) {
            throw new RuntimeException("Not implemented");
        }

    }
}
