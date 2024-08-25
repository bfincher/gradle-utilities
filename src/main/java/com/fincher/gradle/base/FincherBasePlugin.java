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

    private static final String PUBLISH_USERNAME_PROPERTY = "publishUsername";
    private static final String PUBLISH_PASSWORD_PROPERTY = "publishPassword";
    private static final String PUBLISH_SNAPSHOT_URL_PROPERTY = "publishSnapshotUrl";
    private static final String PUBLISH_RELEASE_URL_PROPERTY = "publishReleaseUrl";

    private boolean allowInsecureProtocols = false;

    public void apply(Project project) {
        Object localNexus = project.findProperty("localNexus");

        if (localNexus != null) {
            allowInsecureProtocols = !localNexus.toString().contains("https");

            project.getRepositories().maven(action -> {
                action.setUrl(URI.create(localNexus.toString()));
                if (allowInsecureProtocols) {
                    action.setAllowInsecureProtocol(true);
                }
            });
        }

        project.getRepositories().mavenCentral();

        project.getPluginManager().apply(BasePlugin.class);

        configurePublishing(project);
    }

    private void configurePublishing(Project project) {
        project.getPluginManager().apply(MavenPublishPlugin.class);
        project.getRepositories().mavenLocal();

        // Don't throw errors here if properties don't exist.
        // Throw the exceptions after evaluating the task graph.
        if (project.findProperty(PUBLISH_USERNAME_PROPERTY) != null &&
                project.findProperty(PUBLISH_PASSWORD_PROPERTY) != null &&
                project.findProperty(PUBLISH_SNAPSHOT_URL_PROPERTY) != null &&
                project.findProperty(PUBLISH_RELEASE_URL_PROPERTY) != null) {
            PublishingExtension pe = project.getExtensions().getByType(PublishingExtension.class);

            pe.getRepositories().maven(repo -> {
                String uri = project
                        .findProperty(
                                project.getVersion().toString().endsWith("-SNAPSHOT") ? PUBLISH_SNAPSHOT_URL_PROPERTY
                                        : PUBLISH_RELEASE_URL_PROPERTY)
                        .toString();
                repo.setUrl(URI.create(uri));
                if (allowInsecureProtocols) {
                    repo.setAllowInsecureProtocol(true);
                }

                repo.credentials(creds -> {
                    creds.setUsername(project.findProperty(PUBLISH_USERNAME_PROPERTY).toString());
                    creds.setPassword(project.findProperty(PUBLISH_PASSWORD_PROPERTY).toString());
                });
            });
        }

        project.getGradle().getTaskGraph().whenReady(graph -> graph.getAllTasks().stream()
                .filter(Task::getEnabled)
                .filter(task -> task.getName().equals("publish"))
                .forEach(task -> {
                    Objects.requireNonNull(project.findProperty(PUBLISH_USERNAME_PROPERTY),
                            "The publishUsername property must be set when publishing").toString();

                    Objects.requireNonNull(project.findProperty(PUBLISH_PASSWORD_PROPERTY),
                            "The publishPassword property must be set when publishing").toString();

                    Objects.requireNonNull(project.findProperty(PUBLISH_SNAPSHOT_URL_PROPERTY),
                            "The publishSnapshotUrl property must be set when publishing").toString();

                    Objects.requireNonNull(project.findProperty(PUBLISH_RELEASE_URL_PROPERTY),
                            "The publishReleaseUrl property must be set when publishing").toString();

                }));
    }
}
