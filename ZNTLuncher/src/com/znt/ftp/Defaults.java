package com.znt.ftp;

import java.io.File;

import android.os.Environment;

import com.znt.utils.Constant;

public class Defaults {
	public static int inputBufferSize = 256;
	public static int dataChunkSize = 65536; // do file I/O in 64k chunks
	public static int serverLogScrollBack = 10;
	public static String chrootDir = Environment.getExternalStorageDirectory()
			.getAbsolutePath();// + Constant.MUSIC_DIR;
	public static String username = "znt";
	public static String password = "znt";
	public static int portNumber = 2121;
	public static final int tcpConnectionBacklog = 5;
	public static final boolean stayAwake = false;
	public static final int SO_TIMEOUT_MS = 30000; // socket timeout millis
	// FTP control sessions should start out in ASCII, according to the RFC.
	// However, many clients don't turn on UTF-8 even though they support it,
	// so we just turn it on by default.
	public static final String SESSION_ENCODING = "UTF-8";

	// This is a flag that should be true for public builds and false for dev
	// builds
	public static final boolean release = true;

	public static final boolean do_mediascanner_notify = true;

	public static int getPortNumber() {
		return portNumber;
	}

	public static void createDir()
	{
		File musicFile = new File(chrootDir);
		if(!musicFile.exists())
			musicFile.mkdirs();
		
		File qrFile = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + Constant.WORK_DIR + "/ftp/qrcode/");
		if(!qrFile.exists())
			qrFile.mkdirs();
		
		File logFile = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + Constant.WORK_DIR + "/ftp/error/");
		if(!logFile.exists())
			logFile.mkdirs();
	}
	
	public static void setPortNumber(int portNumber) {
		Defaults.portNumber = portNumber;
	}

	public static void setServerLogScrollBack(int serverLogScrollBack) {
		Defaults.serverLogScrollBack = serverLogScrollBack;
	}

	public static int getInputBufferSize() {
		return inputBufferSize;
	}

	public static void setInputBufferSize(int inputBufferSize) {
		Defaults.inputBufferSize = inputBufferSize;
	}

	public static int getDataChunkSize() {
		return dataChunkSize;
	}

	public static void setDataChunkSize(int dataChunkSize) {
		Defaults.dataChunkSize = dataChunkSize;
	}

	public static int getServerLogScrollBack() {
		return serverLogScrollBack;
	}

	public static void setLogScrollBack(int serverLogScrollBack) {
		Defaults.serverLogScrollBack = serverLogScrollBack;
	}

}
