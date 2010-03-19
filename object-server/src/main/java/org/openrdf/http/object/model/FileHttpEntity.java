package org.openrdf.http.object.model;

import java.io.File;

import org.apache.http.entity.FileEntity;

public class FileHttpEntity extends FileEntity {
	private File file;

	public FileHttpEntity(File file, String contentType) {
		super(file, contentType);
		this.file = file;
	}

	public File getFile() {
		return file;
	}

}
