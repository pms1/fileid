package com.github.pms1.fileid;

import java.io.Serializable;
import java.util.Objects;

public class FileKey implements Serializable {
	private static final long serialVersionUID = 2331573946686690516L;

	public FileKey(String id) {
		Objects.requireNonNull(id);
		this.id = id;
	}

	private final String id;

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return id;
	}
}
