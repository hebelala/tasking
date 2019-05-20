/**
 * Copyright © 2019 hebelala (hebelala@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.hebelala.tasking.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * @author hebelala
 */
public final class FileUtils {

	public static void copyFileToFile(File source, File target) throws IOException {
		target.getParentFile().mkdirs();
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;
		try {
			bis = new BufferedInputStream(new FileInputStream(source));
			bos = new BufferedOutputStream(new FileOutputStream(target));
			int len;
			byte[] bs = new byte[2048];
			while ((len = bis.read(bs)) != -1) {
				bos.write(bs, 0, len);
			}
		} finally {
			CloseableUtils.closeQuietly(bis);
			CloseableUtils.closeQuietly(bos);
		}
	}

	public static void copyToDirectory(File file, File directory) throws IOException {
		if (file.isDirectory()) {
			File targetDirectory = new File(directory, file.getName());
			targetDirectory.mkdirs();
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					copyToDirectory(child, targetDirectory);
				}
			}
		} else {
			directory.mkdirs();
			BufferedInputStream bis = null;
			BufferedOutputStream bos = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(file));
				bos = new BufferedOutputStream(new FileOutputStream(new File(directory, file.getName())));
				int len;
				byte[] bs = new byte[2048];
				while ((len = bis.read(bs)) != -1) {
					bos.write(bs, 0, len);
				}
			} finally {
				CloseableUtils.closeQuietly(bis);
				CloseableUtils.closeQuietly(bos);
			}
		}
	}

	public static void delete(File file) {
		if (file.exists()) {
			if (file.isDirectory()) {
				for (File childFile : file.listFiles()) {
					delete(childFile);
				}
			}
			file.delete();
		}
	}

	public static void unzipToDirectory(File file, File directory) throws IOException {
		ZipFile zipFile = new ZipFile(file);
		try {
			Enumeration<? extends ZipEntry> entries = zipFile.entries();
			while (entries.hasMoreElements()) {
				ZipEntry zipEntry = entries.nextElement();
				File temp = new File(directory.getCanonicalPath() + File.separator
						+ zipEntry.getName().replaceAll("/", File.separator));
				if (zipEntry.isDirectory()) {
					temp.mkdirs();
				} else {
					File parentFile = temp.getParentFile();
					if (!parentFile.exists()) {
						parentFile.mkdirs();
					}
					BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(zipEntry));
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(temp));
					try {
						int len;
						byte[] bs = new byte[2048];
						while ((len = bis.read(bs)) != -1) {
							bos.write(bs, 0, len);
						}
					} finally {
						bis.close();
						bos.close();
					}
				}
			}
		} finally {
			zipFile.close();
		}
	}

	/**
	 * @param fileList      The files need to be packaged
	 * @param file          The target zip file
	 * @param baseDirectory The base directory of the zip file
	 */
	public static void zipToFile(List<File> fileList, File file, String baseDirectory) throws IOException {
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(new FileOutputStream(file));
			ZipEntry root = null;
			if (StringUtils.isNotBlank(baseDirectory)) {
				root = new ZipEntry(baseDirectory + "/");
				zos.putNextEntry(root);
			}
			for (File appFile : fileList) {
				zip0(zos, root, appFile);
			}
		} finally {
			CloseableUtils.closeQuietly(zos);
		}
	}

	private static void zip0(ZipOutputStream zos, ZipEntry parent, File file) throws IOException {
		String zipEntryName0 = parent != null ? parent.getName() + file.getName() : file.getName();
		if (file.isDirectory()) {
			ZipEntry zipEntry = new ZipEntry(zipEntryName0 + "/");
			zos.putNextEntry(zipEntry);
			for (File child : file.listFiles()) {
				zip0(zos, zipEntry, child);
			}
		} else {
			zos.putNextEntry(new ZipEntry(zipEntryName0));
			FileInputStream fis = new FileInputStream(file);
			try {
				int len;
				byte[] bs = new byte[2048];
				while ((len = fis.read(bs)) != -1) {
					zos.write(bs, 0, len);
				}
			} finally {
				fis.close();
			}
		}
	}
}
