package com.fincher.gradle.base;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.tasks.testing.Test;
import org.gradle.plugins.ide.eclipse.model.EclipseClasspath;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;
import org.gradle.testing.jacoco.plugins.JacocoPlugin;

import com.fincher.gradle.checkstyle.CheckstyleConfigPlugin;
import com.fincher.gradle.eclipse.EclipseSettings;

public class FincherJavaPlugin implements Plugin<Project> {

	public void apply(Project project) {
		project.getPluginManager().apply(FincherBasePlugin.class);
		project.getPluginManager().apply(CheckstyleConfigPlugin.class);
		configureJava(project);
		configureEclipse(project);
		configurePublishing(project);
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

}
