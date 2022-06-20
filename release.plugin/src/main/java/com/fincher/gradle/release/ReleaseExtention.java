package com.fincher.gradle.release;

import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;

public abstract class ReleaseExtention {
	
	protected static enum ReleaseType {
		MAJOR, MINOR, PATCH
	}

	public abstract RegularFileProperty getVersionFile();
	
	public abstract Property<String> getVersionKeyValue();
	
//	public abstract Property<String> getReleaseType();

}
