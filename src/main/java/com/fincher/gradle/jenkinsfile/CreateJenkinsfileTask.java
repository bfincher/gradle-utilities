package com.fincher.gradle.jenkinsfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

public abstract class CreateJenkinsfileTask extends DefaultTask {

    @Inject
    public CreateJenkinsfileTask() {
        Project project = getProject();
        getGradleCommand().convention("gradle");
        getOutputFile().convention(new File(project.getProjectDir(), "Jenkinsfile"));

        String defaultGradleOpts = "-s --build-cache";

        if (project.hasProperty("localNexus")) {
            defaultGradleOpts += " -PlocalNexus=" + project.findProperty("localNexus");
        }

        getGradleOptions().convention(defaultGradleOpts);

        setDefaultFromPropertyIfPresent("gitUserEmail", getGitUserEmail());
        setDefaultFromPropertyIfPresent("gitUserName", getGitUserName());
        setDefaultFromPropertyIfPresent("publishReleaseUrl", getPublishReleaseUrl());
        setDefaultFromPropertyIfPresent("publishSnapshotUrl", getPublishSnapshotUrl());
    }

    @Input
    public abstract Property<String> getGradleOptions();

    @Input
    public abstract Property<String> getAgent();

    @Input
    public abstract Property<String> getGitUserEmail();

    @Input
    public abstract Property<String> getGitUserName();

    @Input
    public abstract Property<String> getPublishReleaseUrl();

    @Input
    public abstract Property<String> getPublishSnapshotUrl();

    @Input
    @Optional
    public abstract Property<String> getGradleCommand();

    @InputFile
    @Optional
    public abstract RegularFileProperty getAdditionalParametersFile();

    @Input
    @Optional
    public abstract Property<String> getTools();

    @InputFile
    @Optional
    public abstract RegularFileProperty getBeforeStagesFile();

    @Input
    @Optional
    public abstract RegularFileProperty getPrePrepareFile();

    @Input
    @Optional
    public abstract RegularFileProperty getPostPrepareFile();

    @Input
    @Optional
    public abstract RegularFileProperty getPreBuildStageFile();

    @Input
    public abstract ListProperty<String> getBuildSteps();

    @Input
    @Optional
    public abstract RegularFileProperty getPostBuildStageFile();

    @Input
    @Optional
    public abstract RegularFileProperty getPreFinalizeStepsFile();

    @Input
    @Optional
    public abstract RegularFileProperty getPostFinalizeStepsFile();

    @Input
    @Optional
    public abstract RegularFileProperty getSuffixFile();

    @OutputFile
    @Optional
    public abstract Property<File> getOutputFile();

    @TaskAction
    public void generateFile() throws IOException {
        VelocityEngine ve = new VelocityEngine();
        ve.init();
        VelocityContext ctx = new VelocityContext();

        ctx.put("gradleOpts", getGradleOptions().get());
        ctx.put("agent", getAgent().get());
        ctx.put("gitUserEmail", getGitUserEmail().get());
        ctx.put("gitUserName", getGitUserName().get());
        ctx.put("releaseUrl", getPublishReleaseUrl().get());
        ctx.put("snapshotUrl", getPublishSnapshotUrl().get());
        ctx.put("gradleCmd", getGradleCommand().get());
        ctx.put("additionalParameters", getFileContent(getAdditionalParametersFile(), ""));
        ctx.put("beforeStages", getFileContent(getBeforeStagesFile(), ""));
        ctx.put("prePrepare", getFileContent(getPrePrepareFile(), ""));
        ctx.put("postPrepare", getFileContent(getPostPrepareFile(), ""));
        ctx.put("preBuildStage", getFileContent(getPreBuildStageFile(), ""));
        ctx.put("postBuildStage", getFileContent(getPostBuildStageFile(), ""));
        ctx.put("preFinalizeSteps", getFileContent(getPreFinalizeStepsFile(), ""));
        ctx.put("postFinalizeSteps", getFileContent(getPostFinalizeStepsFile(), ""));
        ctx.put("suffix", getFileContent(getSuffixFile(), ""));
        ctx.put("tools", getTools().getOrElse(""));

        ctx.put("buildSteps",
                "        " + getBuildSteps().get().stream().collect(Collectors.joining("\n" + "        ")));

        Path outputFile = getOutputFile().get().toPath();
        Reader template = new InputStreamReader(
                CreateJenkinsfileTask.class.getClassLoader().getResourceAsStream("Jenkinsfile.vtl"));

        try (Writer writer = Files.newBufferedWriter(outputFile)) {
            ve.evaluate(ctx, writer, "", template);
        }
    }

    private void setDefaultFromPropertyIfPresent(String propertyName, Property<String> taskProperty) {
        Project project = getProject();
        if (project.hasProperty(propertyName)) {
            taskProperty.convention(project.findProperty(propertyName).toString());
        }
    }

    private static String getFileContent(RegularFileProperty prop, String defaultValue) throws IOException {
        if (prop.isPresent()) {
            return Files.readString(prop.get().getAsFile().toPath());
        }

        return defaultValue;
    }
}
