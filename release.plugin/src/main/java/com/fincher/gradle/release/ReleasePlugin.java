package com.fincher.gradle.release;

import java.io.File;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;

import com.fincher.gradle.release.ReleaseExtention.ReleaseType;

public class ReleasePlugin implements Plugin<Project> {
	
	
	
	public void apply(Project project) {
		
		ReleaseExtention extension = project.getExtensions().create("release", ReleaseExtention.class);
		
		extension.getVersionFile().convention(new File(project.getProjectDir(), "gradle.properties"));
		extension.getVersionKeyValue().convention("version");
		
		project.getTasks().register("prepareRelease", PrepareReleaseTask.class, task -> {
			task.getVersionFile().set(extension.getVersionFile().get().toPath());
		});
		
        /*
		PluginContainer plugins = project.getPlugins();
		plugins.withType(JavaPlugin.class, ___ -> {
			plugins.apply(EclipsePlugin.class);
			project.getExtensions().getByType(EclipseModel.class).getJdt().file(merger -> {
				merger.withProperties(properties -> {
					try {
						properties.load(
								ReleasePlugin.class.getClassLoader().getResourceAsStream("default.jdt.core.prefs"));
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				});
			});
			project.getTasks().named(JavaPlugin.COMPILE_JAVA_TASK_NAME, task -> {
				task.dependsOn(EclipsePlugin.ECLIPSE_JDT_TASK_NAME);
			});
		});
            */
	}

//	private File getVersionFile(Project project, ReleaseExtention extension) {
//		File defaultValue = new File(project.getProjectDir(), "settings.gradle");
//
//		RegularFileProperty property = extension.getVersionFile();
//		if (property == null) {
//			return defaultValue;
//		}
//
//		return property.get().getAsFile();
//	}
}
