package com.fincher.gradle.release;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.util.List;

import org.gradle.api.provider.Property;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

public class PrepareReleaseTaskTest extends AbstractReleaseTaskTest<PrepareReleaseTask> {

	@Test
	public void testVersionFileOverride() throws Exception {
		versionFile = projectDir.resolve("otherFile");
		System.out.println("version file = " + versionFile);
		Files.write(versionFile, Lists.newArrayList("some stuff", "version=0.0.1", "some other stuff"));
		task.setProperty("versionFile", versionFile.toFile());
		
		task.releaseTaskAction();
		
		@SuppressWarnings("unchecked")
		Property<String> versionKeyValue = mock(Property.class);
		when(versionKeyValue.get()).thenReturn("version");
		
		VersionFile.load(project, task.getVersionFile(), task.getVersionKeyValue());
		
		System.out.println("releaseType = " + task.releaseType);
		List<String> lines = Files.readAllLines(versionFile);
		assertEquals(3, lines.size());
		assertEquals("version=0.0.2", lines.get(1));
	}

}
