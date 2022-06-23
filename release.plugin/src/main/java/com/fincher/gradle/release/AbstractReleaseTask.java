package com.fincher.gradle.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public abstract class AbstractReleaseTask extends DefaultTask {

	protected Repository repo;
	protected Git git;
	protected VersionFile version;
	protected String relativeVersionFile;

//	@InputDirectory
//	@Optional
//	public abstract Property<File> getBaseRepoDir();

	@InputFile
	@Optional
	public abstract Property<File> getVersionFile();

	@Input
	@Optional
	public abstract Property<String> getVersionKeyValue();

	@TaskAction
	public void releaseTaskAction() throws GitAPIException, IOException {

		Project project = getProject();
		Path projectDir = project.getProjectDir().toPath();

		version = VersionFile.load(getProject(), getVersionFile(), getVersionKeyValue());

		relativeVersionFile = projectDir.relativize(version.getFile()).toString();

		repo = initGitRepo();
		git = initGit(repo);

		verifyNoUncommitedChanges();

		String branch = repo.getBranch();
	}

	protected static String replaceGroup(String source, Matcher matcher, String group, String replacement) {
		return new StringBuilder(source).replace(matcher.start(group), matcher.end(group), replacement).toString();
	}

	protected Repository initGitRepo() throws IOException {
		Path dirToSearch = getProject().getProjectDir().toPath();
		Path testDir = dirToSearch.resolve(".git");
		Path gitDir = null;

		for (int i = 0; i < 5; i++) {
			if (Files.exists(testDir) && Files.isDirectory(testDir)) {
				gitDir = testDir;
				break;
			}
		}
		
		if (gitDir == null) {
			System.err.println("Unable to find .git directory");
			throw new GradleException("Unable to find .git directory");
		}

		return new FileRepositoryBuilder().setGitDir(gitDir.toFile()).build();
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
