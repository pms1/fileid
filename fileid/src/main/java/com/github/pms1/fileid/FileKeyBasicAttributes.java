package com.github.pms1.fileid;

import java.nio.file.attribute.BasicFileAttributes;

public interface FileKeyBasicAttributes extends BasicFileAttributes {
	FileKey fileKey();
}
