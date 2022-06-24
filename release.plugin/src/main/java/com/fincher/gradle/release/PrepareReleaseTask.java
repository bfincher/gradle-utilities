package com.fincher.gradle.release;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.options.Option;

public abstract class PrepareReleaseTask extends AbstractReleaseTask {

    static enum ReleaseType {
        MAJOR, MINOR, PATCH, MANUAL;
    }

    ReleaseType releaseType;
    String releaseVersionOverride = null;

    @Option(option = "releaseType", description = "The type of release.  One of MAJOR, MINOR, PATCH, MANUAL.  If MANUAL is specified, releaseVersion must also be specified")
    public void setReleaseType(ReleaseType releaseType) {
        this.releaseType = releaseType;
    }

    @Option(option = "releaseVersion", description = "Only used with MANUAL release type.   Specifies the version to set for the release")
    public void setReleaseVersion(String releaseVersion) {
        releaseVersionOverride = releaseVersion;
    }

    @Input
    public ReleaseType getReleaseType() {
        return releaseType;
    }

    @Input
    @Optional
    public abstract Property<String> getTagPrefix();

    @Override
    public void releaseTaskAction() throws IOException, GitAPIException {
        super.releaseTaskAction();

        try {
            switch (getReleaseType()) {
            case MAJOR:
                String major = String.valueOf(Integer.parseInt(version.getMajor()) + 1);
                version.replaceMajor(major);
                version.replaceMinor("0");
                version.replacePatch("0");
                version.replaceSuffix("");
                break;

            case MINOR:
                String minor = String.valueOf(Integer.parseInt(version.getMinor()) + 1);
                version.replaceMinor(minor);
                version.replacePatch("0");
                version.replaceSuffix("");
                break;

            case PATCH:
                // If the current version is a snapshot, just remove the snapshot
                if (!version.getSuffix().equals("-SNAPSHOT")) {
                    String patch = String.valueOf(Integer.parseInt(version.getPatch()) + 1);
                    version.replacePatch(patch);
                }

                version.replaceSuffix("");
                break;

            case MANUAL:
                if (releaseVersionOverride == null) {
                    throw new IllegalStateException("releaseVersion must be specified with a MANUAL release type");
                }
                overrideVersion(releaseVersionOverride);
                break;

            default:
                throw new IllegalStateException();
            }

            version.save();

            git.add().addFilepattern(relativeVersionFile).call();

            String newVersion = version.toString();
            git.commit().setMessage(String.format("\"Set version for release to %s\"", newVersion)).call();

            String tag = getTagPrefix().getOrElse("") + newVersion;
            git.tag().setMessage(tag).setName(tag).setAnnotated(true).call();
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }

    }

}
