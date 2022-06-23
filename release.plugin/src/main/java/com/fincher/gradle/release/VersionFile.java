package com.fincher.gradle.release;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

class VersionFile {

	private static final String VERSION_PREFIX_GROUP = "versionPrefix";
	private static final String VERSION_GROUP = "version";
	private static final String MAJOR_GROUP = "major";
	private static final String MINOR_GROUP = "minor";
	private static final String PATCH_GROUP = "patch";
	private static final String SUFFIX_GROUP = "suffix";

	private static final String versionPatternStr = String.format(
			"(?<%s>[\'\"]?(?<%s>\\d+)\\.(?<%s>\\d+)\\.(?<%s>\\d+)(?<%s>[a-zA-Z0-9_-]*)[\'\"]?)", VERSION_GROUP,
			MAJOR_GROUP, MINOR_GROUP, PATCH_GROUP, SUFFIX_GROUP);

	private final Path file;
	private final Pattern pattern;
	private final List<String> fileContent;
	private final int fileContentIndex;
	private String major;
	private String minor;
	private String patch;
	private String suffix;

	private VersionFile(Path file, Pattern pattern, List<String> fileContent, int fileContentIndex, String major,
			String minor, String patch, String suffix) {
		this.file = file;
		this.pattern = pattern;
		this.fileContent = fileContent;
		this.fileContentIndex = fileContentIndex;
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.suffix = suffix;
	}

	// TODO change version key value to a property
	static VersionFile load(Project project, Property<Path> fileProperty, String versionKeyValue) throws IOException {
		Path file = fileProperty.getOrElse(new File(project.getProjectDir(), "gradle.properties").toPath());
		return load(file, versionKeyValue);
	}

	static VersionFile load(Path file, String versionKeyValue) throws IOException {
		List<String> fileContent = Files.readAllLines(file);

		String versionLinePatternStr = String.format("(?<%s>\\s*%s\\s*=\\s*)%s", VERSION_PREFIX_GROUP, versionKeyValue,
				versionPatternStr);
		Pattern pattern = Pattern.compile(versionLinePatternStr);

		int lineIndex = -1;
		String major = null;
		String minor = null;
		String patch = null;
		String suffix = null;

		for (int i = 0; i < fileContent.size(); i++) {
			Matcher m = pattern.matcher(fileContent.get(i));
			if (m.find()) {
				if (lineIndex != -1) {
					throw new IllegalStateException("Multiple lines found matching the version pattern");
				}

				lineIndex = i;
				major = m.group(MAJOR_GROUP);
				minor = m.group(MINOR_GROUP);
				patch = m.group(PATCH_GROUP);
				suffix = m.group(SUFFIX_GROUP);
			}
		}

		if (lineIndex == -1) {
			throw new IllegalStateException("Unable to parse the version");
		}

		return new VersionFile(file, pattern, fileContent, lineIndex, major, minor, patch, suffix);
	}

	String getMajor() {
		return major;
	}

	String getMinor() {
		return minor;
	}

	String getPatch() {
		return patch;
	}

	String getSuffix() {
		return suffix;
	}

	void save() throws IOException {
		Files.write(file, fileContent);
	}

	void replaceMajor(String newValue) {
		replace(MAJOR_GROUP, newValue);
		major = newValue;
	}

	void replaceMinor(String newValue) {
		replace(MINOR_GROUP, newValue);
		minor = newValue;
	}

	void replacePatch(String newValue) {
		replace(PATCH_GROUP, newValue);
		patch = newValue;
	}

	void replaceSuffix(String newValue) {
		replace(SUFFIX_GROUP, newValue);
		suffix = newValue;
	}

	@Override
	public String toString() {
		return String.format("%s.%s.%s%s", major, minor, patch, suffix);
	}

	private void replace(String group, String newValue) {
		String line = fileContent.get(fileContentIndex);
		Matcher m = pattern.matcher(line);
		m.find();

		line = new StringBuilder(line).replace(m.start(group), m.end(group), newValue).toString();
		fileContent.set(fileContentIndex, line);
	}

}
