package com.fincher.gradle.checkstyle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * A simple functional test for the 'testPlugin.greeting' plugin.
 */
class CheckstyleConfigPluginFunctionalTest {

	private static final String PLUGIN_ID = "com.fincher.java.checkstyle";
	private static final List<String> PLUGIN_LINES = List.of("plugins {", "    id('" + PLUGIN_ID + "')",
			"    id('java')", "}");

	private Path projectDir;
	private Path buildDir;
	private Path buildFile;
	private Path settingsFile;
	private GradleRunner runner;

	@BeforeEach
	public void beforeEach() throws Exception {
		projectDir = createEmptyDir(Paths.get("build", "testProjectDir"));
		buildDir = projectDir.resolve("build");
		Files.createDirectories(projectDir);
		buildFile = projectDir.resolve("build.gradle");
		runner = initRunner();

		List<String> buildfileLines = new ArrayList<>(PLUGIN_LINES);
		buildfileLines.add("project.tasks.checkstyleMain.dependsOn(" + CheckstyleConfigPlugin.TASK_NAME + ")");
		buildfileLines.add("project.tasks.checkstyleTest.dependsOn(" + CheckstyleConfigPlugin.TASK_NAME + ")");

		Files.write(buildFile, buildfileLines);

		settingsFile = projectDir.resolve("settings.gradle");
		Files.writeString(settingsFile, "");
	}

	@ParameterizedTest
	@ValueSource(strings = { "checkstyleMain", "checkstyleTest" })
	public void test(String task) {
		runWithArguments(task);
		Path configFile = buildDir.resolve("generated/checkstyleConfig/checkstyle.xml");
		assertTrue(Files.exists(configFile));

	}

	private static Path recursivelyDeleteDir(Path dir) throws IOException {
		if (Files.exists(dir)) {
			Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
		}
		return dir;
	}

	private BuildResult runWithArguments(String... arguments) {
		if (arguments.length > 0) {
			runner.withArguments(arguments);
		}

		return runner.build();
	}

	private GradleRunner initRunner() {
		GradleRunner runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withProjectDir(projectDir.toFile());
		System.out.println(runner.getEnvironment());
		System.out.println(runner.getPluginClasspath());
		return runner;
	}

	/** Create the given directory. If it previously exists, delete and re-create */
	private static Path createEmptyDir(Path dir) throws IOException {
		recursivelyDeleteDir(dir);
		Files.createDirectories(dir);
		return dir;
	}

}
