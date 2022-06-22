package com.fincher.gradle.release;

import java.io.IOException;
import java.util.regex.Matcher;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;

import com.fincher.gradle.release.ReleaseExtention.ReleaseType;

public abstract class PrepareReleaseTask extends AbstractReleaseTask {

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
			Matcher matcher = getVersion();
			String major = matcher.group("major");
			String minor = matcher.group("minor");
			String patch = matcher.group("patch");						

			switch (getReleaseType()) {
			case MAJOR:
				major = String.valueOf(Integer.parseInt(major) + 1);
				minor = "0";
				patch = "0";
				break;

			case MINOR:
				minor = String.valueOf(Integer.parseInt(minor) + 1);
				patch = "0";
				break;

			case PATCH:
				patch = String.valueOf(Integer.parseInt(patch) + 1);
				break;

			default:
				throw new IllegalStateException();
			}

			replaceVersion(major, minor, patch, "");

			git.add().addFilepattern(versionFile.toString()).call();
			String newVersion = String.format("%s.%s.%s", major, minor, patch);
			git.commit().setMessage(String.format("\"Set version for release to %s\"", newVersion));
			
			String tag = getTagPrefix() + newVersion;
			git.tag().setMessage(tag).setName(tag).setAnnotated(true).call();
		} catch (GitAPIException  e) {
			throw new RuntimeException(e);
		}

	}

}
