package com.fincher.gradle.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.impldep.com.google.api.client.repackaged.com.google.common.annotations.VisibleForTesting;

public abstract class AbstractReleaseTask extends DefaultTask {

	private static final String versionPatternStr = "(?<version>(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)(?<suffix>.*))";

	protected Path versionFile;
	private String versionKeyValue = "version";
	protected Repository repo;
	protected Git git;

	public AbstractReleaseTask() {
		versionFile = new File(getProject().getProjectDir(), "gradle.properties").toPath();
	}

//	@Option(option = "versionFile", description = "The file that contains the project's version")
//	public void setVersionFile(String versionFile) {
//		this.versionFile = Paths.get(versionFile);
//	}
//
//	@Option(option = "versionKeyValue", description = "The key part of the key value pair in the version file")
//	public void setVersionKeyValue(String versionKeyValue) {
//		this.versionKeyValue = versionKeyValue;
//	}
//
//	@Input
//	@Optional
//	public String getVersionKeyValue() {
//		return versionKeyValue;
//	}
//
//	@InputFile
//	@Optional
//	public Path getVersionFile() {
//		return versionFile;
//	}

	@InputDirectory
	@Optional
	public abstract Property<File> getBaseRepoDir();

	@TaskAction
	public void releaseTaskAction() throws GitAPIException, IOException {
		repo = initGitRepo();
		git = initGit(repo);

		Status status = git.status().call();
		if (status.hasUncommittedChanges()) {
			System.err.println("Unable to release with uncommitted changes");
			throw new IllegalStateException("Unable to release with uncommitted changes");
		}

		String branch = repo.getBranch();

	}

	@Internal
	protected Matcher getVersion() throws IOException {
		return getVersion(versionFile, versionKeyValue);
	}
	
	
	protected static Matcher getVersion(Path versionFile, String versionKeyValue) throws IOException {
		String versionFileContent = Files.readString(versionFile);
		Pattern versionLinePattern = buildVersionLinePattern(versionKeyValue);
		Matcher matcher = versionLinePattern.matcher(versionFileContent);
		if (matcher.find()) {
			return matcher;
		}

		String errorMsg = "Could not parse version from " + versionFile;
		System.err.println(errorMsg);
		throw new IllegalStateException(errorMsg);
	}

	protected void replaceVersion(String newVersion) throws IOException {
		Matcher matcher = getVersion();
		String replacement = matcher.replaceFirst(newVersion);
		Files.writeString(versionFile, matcher.group("versionPrefix") + replacement);
	}

	protected Repository initGitRepo() throws IOException {
		File gitDir = getBaseRepoDir().getOrElse(new File(getProject().getProjectDir(), ".git"));
		return new FileRepositoryBuilder().setGitDir(gitDir).build();
	}

	protected Git initGit(Repository repo) {
		return new Git(repo);
	}
	
	@VisibleForTesting
	static Pattern buildVersionLinePattern(String versionKeyValue) {
		String versionLinePatternStr = String.format("(?<versionPrefix>\\s*%s\\s*=\\s*)%s", versionKeyValue, versionPatternStr);
		return Pattern.compile(versionLinePatternStr);
	}

}
