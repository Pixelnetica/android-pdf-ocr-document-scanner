package com.pixelnetica.easyscan;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Denis on 23.03.2018.
 */

class TaskResult {
	@Retention(RetentionPolicy.SOURCE)
	@IntDef({NOERROR, OUTOFMEMORY, INVALIDFILE, PROCESSING, NODOCCORNERS, INVALIDCORNERS, CANTSAVEFILE})
	@interface TaskError {};
	static final int NOERROR = 0;
	static final int OUTOFMEMORY = 1;
	static final int INVALIDFILE = 2;
	static final int PROCESSING = 3;
	static final int NODOCCORNERS = 4;
	static final int INVALIDCORNERS = 5;
	static final int CANTSAVEFILE = 6;

	@TaskError final int error;

	TaskResult() {
		this.error = NOERROR;
	}
	TaskResult(@TaskError int error) {
		this.error = error;
	}

	boolean hasError() {
		return error != NOERROR;
	}
}
