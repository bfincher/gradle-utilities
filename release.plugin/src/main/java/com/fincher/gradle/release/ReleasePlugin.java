package com.fincher.gradle.release;

import java.util.function.Supplier;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

public class ReleasePlugin implements Plugin<Project> {
	
	
	private <T> void setTaskPropertyFromExtension(Property<T> source, Supplier<Property<T>> dest) {
		if (source.isPresent()) {
			dest.get().set(source.get());
		}
	}
	
	public void apply(Project project) {
		
		ReleaseExtension extension = project.getExtensions().create("release", ReleaseExtension.class);
		extension.getVersionKeyValue().convention("version");
		
		project.getTasks().register("prepareRelease", PrepareReleaseTask.class, task -> {
			setTaskPropertyFromExtension(extension.getVersionFile(), task::getVersionFile);
			setTaskPropertyFromExtension(extension.getVersionKeyValue(), task::getVersionKeyValue);
			setTaskPropertyFromExtension(extension.getRequiredBranchRegex(), task::getRequiredBranchRegex);
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
