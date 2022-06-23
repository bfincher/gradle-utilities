package com.fincher.gradle.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public abstract class AbstractReleaseTask extends DefaultTask {

	private String versionKeyValue = "version";
	protected Repository repo;
	protected Git git;
	protected VersionFile version;
	protected Path versionFile;

	public AbstractReleaseTask() {
//		versionFile = new File(getProject().getProjectDir(), "gradle.properties").toPath();
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

	@InputFile
	@Optional
	public abstract Property<Path> getVersionFile();

	@TaskAction
	public void releaseTaskAction() throws GitAPIException, IOException {
		Project project = getProject();
		Path projectDir = project.getProjectDir().toPath();
	    versionFile = getVersionFile()	.getOrElse(new File(getProject().getProjectDir(), "gradle.properties").toPath());
	    // we want version file to be a relative path to this project
		versionFile = projectDir.relativize(versionFile);
		
		version = VersionFile.load(getProject(), getVersionFile(), versionKeyValue);
		
		repo = initGitRepo();
		git = initGit(repo);

		verifyNoUncommitedChanges();

		String branch = repo.getBranch();
	}

	protected static String replaceGroup(String source, Matcher matcher, String group, String replacement) {
		return new StringBuilder(source).replace(matcher.start(group), matcher.end(group), replacement).toString();
	}

	protected Repository initGitRepo() throws IOException {
		File gitDir = getBaseRepoDir().getOrElse(new File(getProject().getProjectDir(), ".git"));
		return new FileRepositoryBuilder().setGitDir(gitDir).build();
	}

	protected Git initGit(Repository repo) {
		return new Git(repo);
	}
	
	protected void verifyNoUncommitedChanges() throws GitAPIException {
		verifyNoUncommitedChanges(git);
	}
	
	protected static void verifyNoUncommitedChanges(Git git) throws GitAPIException {
		Status status = git.status().call();
		if (status.hasUncommittedChanges()) {
			System.err.println("Unable to release with uncommitted changes");
			throw new IllegalStateException("Unable to release with uncommitted changes");
		}
	}

}
