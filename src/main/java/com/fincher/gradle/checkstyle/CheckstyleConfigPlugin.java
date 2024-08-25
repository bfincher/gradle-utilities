package com.fincher.gradle.checkstyle;

import java.io.BufferedReader;
import java.io.File;
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
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskContainer;

public class CheckstyleConfigPlugin implements Plugin<Project> {

	static final String TASK_NAME = "copyCheckstyleConfig";

	public abstract static class CopyCheckstyleConfigTask extends DefaultTask {

		Path configDir;

		@Inject
		@SuppressWarnings("squid:S5993")
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
		CheckstyleExtension checkstyleExtension = project.getExtensions().getByType(CheckstyleExtension.class);
		
		DirectoryProperty defaultConfigDir = project.getObjects().directoryProperty();
		defaultConfigDir.set(new File(project.getLayout().getBuildDirectory().getAsFile().get(), "generated/checkstyleConfig"));
		checkstyleExtension.getConfigDirectory().convention(defaultConfigDir);

		TaskContainer tasks = project.getTasks();
		tasks.register(TASK_NAME, CopyCheckstyleConfigTask.class, task -> {
			Path configDir = checkstyleExtension.getConfigDirectory().getAsFile().get().toPath();
			task.setConfigDir(configDir);
		});

		tasks.withType(Checkstyle.class, task -> {
			if (task.getName().equals("checkstyleMain")) {
				task.dependsOn(TASK_NAME);
			} else if (task.getName().equals("checkstyleTest")) {
				task.setEnabled(false);
			}
		});
	}

	public static boolean copyFileFromClasspathIfChanged(String source, Path dest) throws IOException {
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
			return true;
		}
		
		return false;
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
