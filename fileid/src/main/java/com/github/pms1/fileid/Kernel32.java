package com.github.pms1.fileid;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinBase.FILETIME;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIFunctionMapper;
import com.sun.jna.win32.W32APITypeMapper;

public interface Kernel32 extends StdCallLibrary {
	final static Map<String, Object> WIN32API_OPTIONS = new HashMap<String, Object>() {
		private static final long serialVersionUID = 1L;
		{
			put(Library.OPTION_FUNCTION_MAPPER, W32APIFunctionMapper.UNICODE);
			put(Library.OPTION_TYPE_MAPPER, W32APITypeMapper.UNICODE);
		}
	};

	public Kernel32 INSTANCE = (Kernel32) Native.loadLibrary("Kernel32",
			Kernel32.class, WIN32API_OPTIONS);

	int GetLastError();

	/**
	 * typedef struct _BY_HANDLE_FILE_INFORMATION { DWORD dwFileAttributes;
	 * FILETIME ftCreationTime; FILETIME ftLastAccessTime; FILETIME
	 * ftLastWriteTime; DWORD dwVolumeSerialNumber; DWORD nFileSizeHigh; DWORD
	 * nFileSizeLow; DWORD nNumberOfLinks; DWORD nFileIndexHigh; DWORD
	 * nFileIndexLow; } BY_HANDLE_FILE_INFORMATION,
	 * *PBY_HANDLE_FILE_INFORMATION;
	 */

	class BY_HANDLE_FILE_INFORMATION extends Structure {
		public DWORD dwFileAttributes;
		public FILETIME ftCreationTime;
		public FILETIME ftLastAccessTime;
		public FILETIME ftLastWriteTime;
		public DWORD dwVolumeSerialNumber;
		public DWORD nFileSizeHigh;
		public DWORD nFileSizeLow;
		public DWORD nNumberOfLinks;
		public DWORD nFileIndexHigh;
		public DWORD nFileIndexLow;

		static class ByReference extends BY_HANDLE_FILE_INFORMATION implements
				Structure.ByReference {

		};

		static class ByValue extends BY_HANDLE_FILE_INFORMATION implements
				Structure.ByValue {

		}

		@Override
		protected List<?> getFieldOrder() {
			return Arrays.asList("dwFileAttributes", "ftCreationTime",
					"ftLastAccessTime", "ftLastWriteTime",
					"dwVolumeSerialNumber", "nFileSizeHigh", "nFileSizeLow",
					"nNumberOfLinks", "nFileIndexHigh", "nFileIndexLow");
		};
	};

	/**
	 * BOOL WINAPI GetFileInformationByHandle( __in HANDLE hFile, __out
	 * LPBY_HANDLE_FILE_INFORMATION lpFileInformation );
	 */
	boolean GetFileInformationByHandle(HANDLE hFile,
			BY_HANDLE_FILE_INFORMATION lpFileInformation);
}