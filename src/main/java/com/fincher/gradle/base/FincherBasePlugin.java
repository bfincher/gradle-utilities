package com.fincher.gradle.base;

import java.net.URI;
import java.util.Objects;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

public class FincherBasePlugin implements Plugin<Project> {

	public void apply(Project project) {
		Object localNexus = project.findProperty("localNexus");
		if (localNexus == null) {
			project.getRepositories().mavenCentral();
		} else {
			project.getRepositories().maven(action -> action.setUrl(URI.create(localNexus.toString())));
		}

		project.getPluginManager().apply(BasePlugin.class);

		configurePublishing(project);
	}

	private void configurePublishing(Project project) {
		project.getPluginManager().apply(MavenPublishPlugin.class);
		project.getRepositories().mavenLocal();

		// Don't throw errors here if properties don't exist.
		// Throw the exceptions after evaluating the task graph.
		if (project.findProperty("publishUsername") != null &&
				project.findProperty("publishPassword") != null &&
				project.findProperty("publishSnapshotUrl") != null &&
				project.findProperty("publishReleaseUrl") != null) {
			PublishingExtension pe = project.getExtensions().getByType(PublishingExtension.class);

			pe.getRepositories().maven(repo -> {
				String uri = project
						.findProperty(project.getVersion().toString().endsWith("-SNAPSHOT") ? "publishSnapshotUrl"
								: "publishReleaseUrl")
						.toString();
				repo.setUrl(URI.create(uri));

				repo.credentials(creds -> {
					creds.setUsername(project.findProperty("publishUsername").toString());
					creds.setPassword(project.findProperty("publishPassword").toString());
				});
			});
		}

		project.getGradle().getTaskGraph().whenReady(graph -> {
			graph.getAllTasks().stream()
					.filter(Task::getEnabled)
					.filter(task -> task.getName().equals("publish"))
					.forEach(task -> {
						Objects.requireNonNull(project.findProperty("publishUsername"),
								"The publishUsername property must be set when publishing").toString();

						Objects.requireNonNull(project.findProperty("publishPassword"),
								"The publishPassword property must be set when publishing").toString();

						Objects.requireNonNull(project.findProperty("publishSnapshotUrl"),
								"The publishSnapshotUrl property must be set when publishing").toString();

						Objects.requireNonNull(project.findProperty("publishReleaseUrl"),
								"The publishReleaseUrl property must be set when publishing").toString();

					});
		});
	}
}
