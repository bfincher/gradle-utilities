package com.fincher.gradle.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git;
import org.gradle.internal.impldep.org.eclipse.jgit.api.Status;
import org.gradle.internal.impldep.org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.internal.impldep.org.eclipse.jgit.lib.Repository;
import org.gradle.internal.impldep.org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public abstract class AbstractReleaseTask extends DefaultTask {

	private static final String versionPatternStr = "(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)(?<suffix>.*)";

	private Pattern versionLinePattern;
	protected Path versionFile;
	private String versionKeyValue = "version";
	private File baseGitRepoDir;
	protected Repository repo;
	protected Git git;

	public AbstractReleaseTask() {
		versionFile = new File(getProject().getProjectDir(), "gradle.properties").toPath();
		baseGitRepoDir = getProject().getProjectDir();
	}

	@Option(option = "versionFile", description = "The file that contains the project's version")
	public void setVersionFile(String versionFile) {
		this.versionFile = Paths.get(versionFile);
	}

	@Option(option = "versionKeyValue", description = "The key part of the key value pair in the version file")
	public void setVersionKeyValue(String versionKeyValue) {
		this.versionKeyValue = versionKeyValue;
	}

	@Input
	@Optional
	public String getVersionKeyValue() {
		return versionKeyValue;
	}

	@InputFile
	@Optional
	public Path getVersionFile() {
		return versionFile;
	}

	@Option(option = "baseGitRepoDir", description = "The directory that contains the .git directory")
	public void setBaseGitRepoDir(String baseGitRepoDir) {
		this.baseGitRepoDir = new File(baseGitRepoDir);
	}

	@InputFile
	@Optional
	public File getBaseRepoDir() {
		return baseGitRepoDir;
	}

	@TaskAction
	public void releaseTaskAction() throws GitAPIException, IOException {
		String versionLinePatternStr = String.format("\\s*%s\\s*=\\s*%s", getVersionKeyValue(), versionPatternStr);
		System.out.println(versionLinePatternStr);
		versionLinePattern = Pattern.compile(versionLinePatternStr);
		
		repo = new FileRepositoryBuilder().setGitDir(baseGitRepoDir).build();
		git = new Git(repo);
		
		Status status = git.status().call();
		if (status.hasUncommittedChanges()) {
			System.err.println("Unable to release with uncommitted changes");
			throw new IllegalStateException("Unable to release with uncommitted changes");
		}
		
		String branch = repo.getBranch();

	}

	@Internal
	protected Matcher getVersion() throws IOException {
		String versionFileContent = Files.readString(versionFile);
		System.out.println("versionFileContent = " + versionFileContent);
		Matcher matcher = versionLinePattern.matcher(versionFileContent);
		if (matcher.find()) {
			return matcher;
		}

		String errorMsg = "Could not parse version from " + versionFile;
		System.err.println(errorMsg);
		throw new IllegalStateException();

	}

	protected void replaceVersion(String newVersion) throws IOException {
		Matcher matcher = getVersion();
		String replacement = matcher.replaceFirst(newVersion);
		Files.writeString(versionFile, replacement);
	}

}
