package com.fincher.gradle.checkstyle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.TaskAction;

public class CheckstyleConfigPlugin implements Plugin<Project> {
	
	static final String taskName = "copyCheckstyleConfig";
	
	abstract static class CopyCheckstyleConfigTask extends DefaultTask {
		
		Path configDir;
		
		@Inject
		public CopyCheckstyleConfigTask() {
		}
		
		void setConfigDir(Path configDir) {
			this.configDir = configDir;
		}
		
		@TaskAction
		public void copyConfig() throws IOException {
			if (!Files.exists(configDir)) {
				Files.createDirectories(configDir);
			}

			Path checkstyleXml = configDir.resolve("checkstyle.xml");
			Path suppressionsXml = configDir.resolve("suppressions.xml");

			copyFileFromClasspathIfChanged("checkstyle.xml", checkstyleXml);
			copyFileFromClasspathIfChanged("suppressions.xml", suppressionsXml);
		}
	}
	
	public void apply(Project project) {
		PluginContainer plugins = project.getPlugins();
		plugins.apply(CheckstylePlugin.class);
		
		project.getTasks().register(taskName, CopyCheckstyleConfigTask.class, task -> {
			CheckstyleExtension parentExtension = project.getExtensions().getByType(CheckstyleExtension.class);
		    Path configDir = parentExtension.getConfigDirectory().getAsFile().get().toPath();
		    task.setConfigDir(configDir);
		});
	}

	private static void copyFileFromClasspathIfChanged(String source, Path dest) throws IOException {
		List<String> sourceContents = readFileFromClassloader(source);
		boolean copy = false;

		if (!Files.exists(dest)) {
			copy = true;
		} else {
			List<String> destContents = Files.readAllLines(dest);
			copy = !sourceContents.equals(destContents);
		}

		if (copy) {
			Files.write(dest, sourceContents);
		}
	}

	private static List<String> readFileFromClassloader(String name) throws IOException {
		List<String> list = new LinkedList<>();

		try (BufferedReader input = new BufferedReader(
				new InputStreamReader(CheckstyleConfigPlugin.class.getClassLoader().getResourceAsStream(name)))) {

			String str;
			while ((str = input.readLine()) != null) {
				list.add(str);
			}

			return list;
		}
	}
}
