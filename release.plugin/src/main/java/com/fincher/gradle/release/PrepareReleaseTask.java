package com.fincher.gradle.release;

import java.io.IOException;
import java.util.regex.Matcher;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;
import org.gradle.internal.impldep.org.eclipse.jgit.api.errors.GitAPIException;

import com.fincher.gradle.release.ReleaseExtention.ReleaseType;

public class PrepareReleaseTask extends AbstractReleaseTask {

	private ReleaseType releaseType;
	private String tagPrefix = "";

	@Option(option = "releaseType", description = "The type of release.  One of MAJOR, MINOR, PATCH")
	public void setReleaseType(ReleaseType releaseType) {
		this.releaseType = releaseType;
	}

	@Input
	public ReleaseType getReleaseType() {
		return releaseType;
	}

	@Option(option = "tagPrefix", description = "An optional prefix to be prepended to the created tag")
	public void setTagPrefix(String tagPrefix) {
		this.tagPrefix = tagPrefix;
	}

	@Input
	public String getTagPrefix() {
		return tagPrefix;
	}

	@Override
	public void releaseTaskAction() throws IOException, GitAPIException {
		super.releaseTaskAction();

		try {
			Matcher oldVersionMatcher = getVersion();
			String major = oldVersionMatcher.group("major");
			String minor = oldVersionMatcher.group("minor");
			String patch = oldVersionMatcher.group("patch");
			String suffix = oldVersionMatcher.group("suffix");

			String newVersion;

			switch (getReleaseType()) {
			case MAJOR:
				newVersion = String.format("%s.%s.%s%s", Integer.parseInt(major) + 1, minor, patch, suffix);
				break;

			case MINOR:
				newVersion = String.format("%s.%s.%s%s", major, Integer.parseInt(minor) + 1, patch, suffix);
				break;

			case PATCH:
				newVersion = String.format("%s.%s.%s%s", major, minor, Integer.parseInt(patch) + 1, suffix);
				break;

			default:
				throw new IllegalStateException();
			}

			replaceVersion(newVersion);

			git.add().addFilepattern(versionFile.toString()).call();
			git.commit().setMessage(String.format("\"Set version for release to %s\"", newVersion));
			
			String tag = getTagPrefix() + newVersion;
			git.tag().setMessage(tag).setName(tag).setAnnotated(true).call();
		} catch (GitAPIException  e) {
			throw new RuntimeException(e);
		}

	}

}
