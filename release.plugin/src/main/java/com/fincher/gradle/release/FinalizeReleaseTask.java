package com.fincher.gradle.release;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.tasks.options.Option;

public abstract class FinalizeReleaseTask extends AbstractReleaseTask {
	
	private String newVersionOverride = null;

	@Option(option = "newVersion", description = "Sets the new release value.  If the prepareRelease set the release version to 1.0.1, the default new release value would be 1.0.1-SNAPSHOT")
	public void setNewVersion(String newVersion) {
		newVersionOverride = newVersion;
	}

	@Override
	public void releaseTaskAction() throws IOException, GitAPIException {
		super.releaseTaskAction();
		
		if (newVersionOverride == null) {
			version.replacePatch(String.valueOf(Integer.parseInt(version.getPatch()) + 1));
			version.replaceSuffix("-SNAPSHOT");
		} else {			
			Pattern p = Pattern.compile(VersionFile.versionPatternStr);
			Matcher m = p.matcher(newVersionOverride);
			if (!m.find()) {
				String errorMsg = String.format("The new version of %s does not match the pattern %s", newVersionOverride, VersionFile.versionPatternStr);
				throw new IllegalArgumentException(errorMsg);
			}
			
			version.replaceMajor(m.group(VersionFile.MAJOR_GROUP));
			version.replaceMinor(m.group(VersionFile.MINOR_GROUP));
			version.replacePatch(m.group(VersionFile.PATCH_GROUP));
			version.replaceSuffix(m.group(VersionFile.SUFFIX_GROUP));
		}
		
		version.save();
		
		git.add().addFilepattern(relativeVersionFile).call();

		String newVersion = version.toString();
		git.commit().setMessage(String.format("\"Set version after release to %s\"", newVersion)).call();
		git.push().setPushTags().call();
	}

}
