package com.fincher.gradle.release;

import java.io.File;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

public abstract class ReleaseExtension {

	protected static enum ReleaseType {
		MAJOR, MINOR, PATCH;

		private final String group;

		private ReleaseType() {
			group = name().toLowerCase();
		}

		public String getRegexGroup() {
			return group;
		}
	}

	@InputFile
	public abstract Property<File> getVersionFile();

	@Input
	public abstract Property<String> getVersionKeyValue();

//	public abstract Property<String> getReleaseType();

}
