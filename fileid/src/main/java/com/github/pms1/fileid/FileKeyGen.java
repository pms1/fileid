package com.github.pms1.fileid;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

import com.github.pms1.fileid.Kernel32.BY_HANDLE_FILE_INFORMATION;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT.HANDLE;

public class FileKeyGen {
	private static class XBasicFileAttributes implements BasicFileAttributes {
		private final FileTime mtime;
		private final FileTime atime;
		private final FileTime ctime;
		private final long size;
		private final FileKey fileKey;

		XBasicFileAttributes(FileTime mtime, FileTime atime, FileTime ctime, long size, FileKey fileKey) {
			this.mtime = mtime;
			this.atime = atime;
			this.ctime = ctime;
			this.size = size;
			this.fileKey = fileKey;
		}

		public FileTime lastModifiedTime() {
			return mtime;
		}

		public FileTime lastAccessTime() {
			return atime;
		}

		public FileTime creationTime() {
			return ctime;
		}

		public boolean isRegularFile() {
			throw new UnsupportedOperationException();
		}

		public boolean isDirectory() {
			throw new UnsupportedOperationException();
		}

		public boolean isSymbolicLink() {
			throw new UnsupportedOperationException();
		}

		public boolean isOther() {
			throw new UnsupportedOperationException();
		}

		public long size() {
			return size;
		}

		public Object fileKey() {
			return fileKey;
		}

	}

	private static final long EPOCH_DIFF_MS = 11644473600000L * 1000;

	/**
	 * @see FILETIME.filetimeToDate
	 */
	private static FileTime convert(FILETIME ft) {
		final long filetime = (long) ft.dwHighDateTime << 32 | ft.dwLowDateTime & 0xffffffffL;
		final long ms_since_16010101 = filetime / 10;
		final long ms_since_19700101 = ms_since_16010101 - EPOCH_DIFF_MS;
		return FileTime.from(ms_since_19700101, TimeUnit.MICROSECONDS);
	}

	public static BasicFileAttributes getFileKeyBasicAttributes(Path p) throws IOException {
		// http://msdn.microsoft.com/en-us/library/windows/desktop/aa363858%28v=vs.85%29.aspx
		final int FILE_SHARE_READ = (0x00000001);
		// final int FILE_SHARE_WRITE = (0x00000002);
		// final int FILE_SHARE_DELETE = (0x00000004);
		final int OPEN_EXISTING = (3);
		final int GENERIC_READ = (0x80000000);
		// final int GENERIC_WRITE = (0x40000000);
		// final int FILE_FLAG_NO_BUFFERING = (0x20000000);
		// final int FILE_FLAG_WRITE_THROUGH = (0x80000000);
		// final int FILE_READ_ATTRIBUTES = (0x0080);
		// final int FILE_WRITE_ATTRIBUTES = (0x0100);
		// final int ERROR_INSUFFICIENT_BUFFER = (122);
		final int FILE_ATTRIBUTE_ARCHIVE = (0x20);

		WinBase.SECURITY_ATTRIBUTES attr = null;
		BY_HANDLE_FILE_INFORMATION lpFileInformation = new BY_HANDLE_FILE_INFORMATION();
		HANDLE hFile = null;

		try {
			String p1 = p.toString();
			if (p1.length() > Kernel32.MAX_PATH) {
				if (p1.startsWith("\\\\")) {
					p1 = "\\\\?\\UNC" + p1.substring(1);
				} else {
					p1 = "\\\\?\\" + p1;
				}
			}
			hFile = Kernel32.INSTANCE.CreateFile(p1, GENERIC_READ, FILE_SHARE_READ, attr, OPEN_EXISTING,
					FILE_ATTRIBUTE_ARCHIVE, null);

			if (WinBase.INVALID_HANDLE_VALUE.equals(hFile))
				throw toIOException(p, "CreateFile('" + p + "') failed", Kernel32.INSTANCE.GetLastError());

			// FileTime time = Files.getLastModifiedTime(Paths.get(args[0]));
			// System.err.println("T=" + time.to(TimeUnit.SECONDS));

			if (!com.github.pms1.fileid.Kernel32.INSTANCE.GetFileInformationByHandle(hFile, lpFileInformation))
				throw toIOException(p, "GetFileInformationByHandle(" + hFile + ") failed",
						Kernel32.INSTANCE.GetLastError());

			if (false) {
				// we treat the unsigned int as long
				long serial = lpFileInformation.dwVolumeSerialNumber.intValue() & 0x00000000ffffffffL;

				System.out.println("\tserial " + Integer.toHexString(lpFileInformation.dwVolumeSerialNumber.intValue())
						+ " " + serial + " " + lpFileInformation.dwVolumeSerialNumber.intValue());

				System.out
						.println(
								"\tmtime "
										+ FILETIME
												.filetimeToDate(lpFileInformation.ftLastWriteTime.dwHighDateTime,
														lpFileInformation.ftLastWriteTime.dwLowDateTime)
												.getTime() / 1000);

				System.out.println("\tfileIndexHigh " + String.format("%08x %10d",
						lpFileInformation.nFileIndexHigh.intValue(), lpFileInformation.nFileIndexHigh.intValue()));
				System.out.println("\tfileIndexLow  " + String.format("%08x %10d",
						lpFileInformation.nFileIndexLow.intValue(), lpFileInformation.nFileIndexLow.intValue()));

				long l = (lpFileInformation.nFileIndexHigh.longValue()) << 32
						| lpFileInformation.nFileIndexLow.intValue();
				System.out.println("\tfileIndex  " + String.format("%16x %20d", l, l));
			}

			long size = (lpFileInformation.nFileSizeHigh.longValue()) << 32 | lpFileInformation.nFileSizeLow.intValue();

			FileKey fileKey = new FileKey(String.format("%x:%x:%x", lpFileInformation.dwVolumeSerialNumber.intValue(),
					lpFileInformation.nFileIndexHigh.intValue(), lpFileInformation.nFileIndexLow.intValue()));

			return new XBasicFileAttributes(convert(lpFileInformation.ftLastWriteTime),
					convert(lpFileInformation.ftLastAccessTime), convert(lpFileInformation.ftCreationTime), size,
					fileKey);
		} catch (RuntimeException | IOException | Error e) {
			// in case of error, close and add a suppressed exception
			if (hFile != null && !WinBase.INVALID_HANDLE_VALUE.equals(hFile)) {
				if (!Kernel32.INSTANCE.CloseHandle(hFile))
					e.addSuppressed(
							toIOException(p, "CloseHandle(" + hFile + ") failed", +Kernel32.INSTANCE.GetLastError()));
				hFile = null;
			}
			throw e;
		} finally {
			// in case of success, fail if close fails
			if (hFile != null && !WinBase.INVALID_HANDLE_VALUE.equals(hFile))
				if (!Kernel32.INSTANCE.CloseHandle(hFile))
					throw toIOException(p, "CloseHandle(" + hFile + ") failed", Kernel32.INSTANCE.GetLastError());

		}
	}

	private static IOException toIOException(Path p, String message, int lastError) {
		switch (lastError) {
		case WinError.ERROR_FILE_NOT_FOUND:
		case WinError.ERROR_PATH_NOT_FOUND:
			return new NoSuchFileException(p.toString(), null, message);
		case WinError.ERROR_SHARING_VIOLATION:
			return new IOException(message + ": " + p + ": GetLastError()=" + lastError + " (ERROR_SHARING_VIOLATION)"
					+ ": " + Kernel32Util.formatMessageFromLastErrorCode(lastError));
		case WinError.ERROR_NETNAME_DELETED:
			return new IOException(message + ": " + p + ": GetLastError()=" + lastError + " (ERROR_NETNAME_DELETED)"
					+ ": " + Kernel32Util.formatMessageFromLastErrorCode(lastError));
		default:
			return new IOException(message + ": " + p + ": GetLastError()=" + lastError + ": "
					+ Kernel32Util.formatMessageFromLastErrorCode(lastError));
		}
	}
}