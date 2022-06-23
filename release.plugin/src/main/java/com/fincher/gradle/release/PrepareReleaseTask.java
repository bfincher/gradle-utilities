package com.fincher.gradle.release;

import java.io.IOException;

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
			switch (getReleaseType()) {
			case MAJOR:
				String major = String.valueOf(Integer.parseInt(versionFile.getMajor()) + 1);
				versionFile.replaceMajor(major);
				versionFile.replaceMinor("0");
				versionFile.replacePatch("0");				
				break;

			case MINOR:
				String minor = String.valueOf(Integer.parseInt(versionFile.getMinor()) + 1);
				versionFile.replaceMinor(minor);
				versionFile.replacePatch("0");
				break;

			case PATCH:
				String patch = String.valueOf(Integer.parseInt(versionFile.getPatch()) + 1);
				versionFile.replacePatch(patch);
				break;

			default:
				throw new IllegalStateException();
			}
			
			versionFile.replaceSuffix("");
			versionFile.save();

			git.add().addFilepattern(versionFile.toString()).call();
			String newVersion = versionFile.toString();
			git.commit().setMessage(String.format("\"Set version for release to %s\"", newVersion));
			
			String tag = getTagPrefix() + newVersion;
			git.tag().setMessage(tag).setName(tag).setAnnotated(true).call();
		} catch (GitAPIException  e) {
			throw new RuntimeException(e);
		}

	}

}
