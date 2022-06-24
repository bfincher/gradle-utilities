package com.fincher.gradle.release;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.TagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public abstract class BaseReleaseTaskTest<T extends AbstractReleaseTask> {
	
	static final String INITIAL_VERSION = "0.0.1-SNAPSHOT";

	T task;
	Project project;
	Path projectDir;
	Path versionFile;

	@Mock
	Repository repo;
	@Mock
	Git git;
	@Mock
	StatusCommand statusCmd;
	@Mock
	Status status;
	@Mock
	AddCommand addCommand;
	@Mock
	CommitCommand commitCommand;
	@Mock
	PushCommand pushCommand;
	@Mock
	TagCommand tagCommand;

	@BeforeEach
	public void beforeEach() throws GitAPIException, IOException {
		MockitoAnnotations.openMocks(this);
		initMocks();

		project = ProjectBuilder.builder().build();
		projectDir = project.getProjectDir().toPath();
		versionFile = projectDir.resolve("gradle.properties");
		Files.writeString(versionFile, "version=" + INITIAL_VERSION);
		
		project.getTasks().register(getTaskName(), getTaskClass());
		Task t = project.getTasks().findByName(getTaskName());
		assertNotNull(t);

		task = getTaskClass().cast(t);
		task.setJGitRepoFactory(() -> repo);
		task.setGitFactory((___) -> git);
	}

	void initMocks() throws GitAPIException, IOException {
		when(git.status()).thenReturn(statusCmd);
		when(statusCmd.call()).thenReturn(status);
		when(status.hasUncommittedChanges()).thenReturn(false);
		
		when(repo.getBranch()).thenReturn("master");
		
		when(git.add()).thenReturn(addCommand);
		when(addCommand.addFilepattern(anyString())).thenReturn(addCommand);
		
		when(git.commit()).thenReturn(commitCommand);
		when(commitCommand.setMessage(anyString())).thenReturn(commitCommand);
		
		when(git.push()).thenReturn(pushCommand);
		when(pushCommand.setPushTags()).thenReturn(pushCommand);
		
		when(git.tag()).thenReturn(tagCommand);
		when(tagCommand.setMessage(anyString())).thenReturn(tagCommand);
		when(tagCommand.setName(anyString())).thenReturn(tagCommand);
		when(tagCommand.setAnnotated(anyBoolean())).thenReturn(tagCommand);
	}

	abstract String getTaskName();

	abstract Class<T> getTaskClass();

}
