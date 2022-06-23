package com.fincher.gradle.release;

import java.io.File;

import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;

public abstract class ReleaseExtension {

	@InputFile
	public abstract Property<File> getVersionFile();

	@Input
	public abstract Property<String> getVersionKeyValue();
	
	@Input
	public abstract Property<String> getRequiredBranchRegex();
	
	@Input
	public abstract Property<String> getTagPrefix();

}
