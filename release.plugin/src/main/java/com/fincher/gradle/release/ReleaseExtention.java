package com.fincher.gradle.release;

import java.io.File;
import java.nio.file.Path;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.InputFile;

public abstract class ReleaseExtention {
	
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
	
	public abstract Property<String> getVersionKeyValue();
	
//	public abstract Property<String> getReleaseType();

}
