/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.fincher.gradle.release;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.gradle.api.Project;
import org.gradle.internal.impldep.org.eclipse.jgit.api.Git;
import org.gradle.internal.impldep.org.eclipse.jgit.lib.Repository;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AbstractReleaseTaskTest {
	
	@Mock
	private Repository repo;
	
	@Mock
	private Git git;
	
	@BeforeEach
	public void beforeEach() {
		MockitoAnnotations.openMocks(this);
	}
	
	@Test
	public void pluginRegistersATask() {
		// Create a test project and apply the plugin
//		Project project = ProjectBuilder.builder().build();
//		project.getTasks().register("abstractReleaseTask", TestClass.class);
//		project.getTasks().findByName("abstractReleaseTask");
//		project.getPlugins().apply("testPlugin.greeting");
//
//		// Verify the result
//		assertNotNull(project.getTasks().findByName("greeting"));
	}
	
	class TestClass extends AbstractReleaseTask {
		protected Repository initGitRepo() throws IOException {
			return repo;
		}
		
		protected Git initGit(Repository repo) {
			return git;
		}
	}
}
