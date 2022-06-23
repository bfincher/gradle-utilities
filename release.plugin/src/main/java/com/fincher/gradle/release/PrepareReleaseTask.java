package com.fincher.gradle.release;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.options.Option;

import com.fincher.gradle.release.ReleaseExtension.ReleaseType;

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
			switch (getReleaseType()) {
			case MAJOR:
				String major = String.valueOf(Integer.parseInt(version.getMajor()) + 1);
				version.replaceMajor(major);
				version.replaceMinor("0");
				version.replacePatch("0");				
				break;

			case MINOR:
				String minor = String.valueOf(Integer.parseInt(version.getMinor()) + 1);
				version.replaceMinor(minor);
				version.replacePatch("0");
				break;

			case PATCH:
				String patch = String.valueOf(Integer.parseInt(version.getPatch()) + 1);
				version.replacePatch(patch);
				break;

			default:
				throw new IllegalStateException();
			}
			
			version.replaceSuffix("");
			version.save();
			
			git.add().addFilepattern(relativeVersionFile).call();
			
			String newVersion = version.toString();
			git.commit().setMessage(String.format("\"Set version for release to %s\"", newVersion)).call();
			
			String tag = getTagPrefix() + newVersion;
			git.tag().setMessage(tag).setName(tag).setAnnotated(true).call();
		} catch (GitAPIException  e) {
			throw new RuntimeException(e);
		}

	}

}
