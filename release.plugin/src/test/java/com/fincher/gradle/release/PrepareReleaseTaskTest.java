package com.fincher.gradle.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.AdditionalAnswers.returnsFirstArg;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.gradle.api.provider.Property;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import com.fincher.gradle.release.PrepareReleaseTask.ReleaseType;
import com.google.common.collect.Lists;

public class PrepareReleaseTaskTest extends BaseReleaseTaskTest<PrepareReleaseTask> {

	@Mock
	private Property<String> versionKeyValueProperty;

	@Mock
	private Property<File> versionFileProperty;	

	@BeforeEach
	@Override
	public void beforeEach() throws IOException, GitAPIException {
		super.beforeEach();

		when(versionKeyValueProperty.getOrElse(anyString())).then(returnsFirstArg());
		when(versionFileProperty.getOrElse(any(File.class))).then(returnsFirstArg());
	}

	@Test
	public void testMajor() throws Exception {
		task.setReleaseType(ReleaseType.MAJOR);
		task.releaseTaskAction();
		verifyResults("1.0.0");
	}
	
	@Test
	public void testMinor() throws Exception {
		task.setReleaseType(ReleaseType.MINOR);
		task.releaseTaskAction();
		verifyResults("0.1.0");
	}
	
	@Test
	public void testPatch() throws Exception {
		task.setReleaseType(ReleaseType.PATCH);
		task.releaseTaskAction();
		verifyResults("0.0.2");
	}
	
	@Test
	public void testManual() throws Exception {
		task.setReleaseType(ReleaseType.MANUAL);
		task.setReleaseVersion("1.1.1");
		task.releaseTaskAction();
		verifyResults("1.1.1");
	}
	
	@Test
	public void testManualWithoutVersion() throws Exception {
		task.setReleaseType(ReleaseType.MANUAL);
		assertThrows(IllegalStateException.class, () -> task.releaseTaskAction());
		verifyNoResults();
	}

	@Test
	public void testVersionFileOverride() throws Exception {
		versionFile = projectDir.resolve("otherFile");
		when(versionFileProperty.getOrElse(any())).thenReturn(versionFile.toFile());		
		
		Files.write(versionFile, Lists.newArrayList("some stuff", "version=0.0.1", "some other stuff"));
		task.setProperty("versionFile", versionFile.toFile());

		task.setReleaseType(ReleaseType.PATCH);
		task.releaseTaskAction();

		@SuppressWarnings("unchecked")
		Property<String> versionKeyValue = mock(Property.class);
		when(versionKeyValue.get()).thenReturn("version");

		// do extra checking of the file here to ensure that the original structure is still in place
		System.out.println("releaseType = " + task.releaseType);
		List<String> lines = Files.readAllLines(versionFile);
		assertEquals(3, lines.size());
		assertEquals("version=0.0.2", lines.get(1));
		
		verifyResults("0.0.2");
	}
	
	@Test
	public void testVersionKeyValueOverride() throws Exception {
		String key = "otherVersion";
		when(versionKeyValueProperty.getOrElse(anyString())).thenReturn(key);
		task.getVersionKeyValue().set(key);
		Files.write(versionFile, Lists.newArrayList("some stuff", key + " = '0.0.1'", "some other stuff"));
		
		task.setReleaseType(ReleaseType.PATCH);
		task.releaseTaskAction();
		
		// do extra checking of the file here to ensure that the original structure is still in place
		System.out.println("releaseType = " + task.releaseType);
		List<String> lines = Files.readAllLines(versionFile);
		assertEquals(3, lines.size());
		assertEquals(key + " = '0.0.2'", lines.get(1));
		
		verifyResults("0.0.2");
	}
	
	@Test
	public void testTagPrefixOverride() throws Exception {
		task.getTagPrefix().set("testTagPrefix");
		task.setReleaseType(ReleaseType.MAJOR);
		task.releaseTaskAction();
		verifyResults("1.0.0", "testTagPrefix");
	}
	
	@Test
	public void testMainBranch() throws Exception {
		when(repo.getBranch()).thenReturn("main");
		task.setReleaseType(ReleaseType.PATCH);
		task.releaseTaskAction();
		verifyResults("0.0.2");
	}
	
	@Test
	public void testInvalidBranch() throws Exception {
		when(repo.getBranch()).thenReturn("other1");
		task.setReleaseType(ReleaseType.PATCH);
		assertThrows(IllegalStateException.class, () -> task.releaseTaskAction());
		verifyNoResults();
	}
	
	@ParameterizedTest
	@ValueSource(strings = {"other1", "other2"})
	public void testBranchOverride(String branch) throws Exception {
		when(repo.getBranch()).thenReturn(branch);
		task.getRequiredBranchRegex().set("(other1)|(other2)");
		
		task.setReleaseType(ReleaseType.PATCH);
		task.releaseTaskAction();
		verifyResults("0.0.2");
	}

	@Override
	String getTaskName() {
		return "prepareRelease";
	}

	@Override
	Class<PrepareReleaseTask> getTaskClass() {
		return PrepareReleaseTask.class;
	}

	private void verifyResults(String expectedVersion) throws Exception {
		verifyResults(expectedVersion, "");
	}

	private void verifyResults(String expectedVersion, String tagPrefix) throws Exception {
		VersionFile version = VersionFile.load(project, versionFileProperty, versionKeyValueProperty);
		assertEquals(expectedVersion, version.toString());
		verify(git).add();
		verify(addCommand).addFilepattern(versionFile.getFileName().toString());
		verify(addCommand).call();

		verify(git).commit();
		verify(commitCommand).setMessage(String.format("\"Set version for release to %s\"", expectedVersion));
		verify(commitCommand).call();

		verify(git).tag();
		verify(tagCommand).setMessage(tagPrefix + expectedVersion);
		verify(tagCommand).setName(tagPrefix + expectedVersion);
		verify(tagCommand).setAnnotated(true);
		verify(tagCommand).call();
	}
	
	private void verifyNoResults() throws Exception {
		VersionFile version = VersionFile.load(project, versionFileProperty, versionKeyValueProperty);
		assertEquals(INITIAL_VERSION, version.toString());
		verify(git, never()).add();
		verify(git, never()).commit();
		verify(git, never()).tag();
	}

}
