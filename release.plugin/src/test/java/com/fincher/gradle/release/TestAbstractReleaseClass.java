package com.fincher.gradle.release;

import java.io.IOException;

import javax.inject.Inject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;

import com.fincher.gradle.release.AbstractReleaseTaskTest.Mocks;

abstract class TestAbstractReleaseClass extends AbstractReleaseTask {
	
	Mocks mocks;
	
	@Inject
	public TestAbstractReleaseClass() {
	}
	
	void setMocks(Mocks mocks) {
		this.mocks = mocks;
	}

	@Override
	protected Repository initGitRepo() throws IOException {
		return mocks.repo;
	}

	@Override
	protected Git initGit(Repository repo) {
		return mocks.git;
	}

}
