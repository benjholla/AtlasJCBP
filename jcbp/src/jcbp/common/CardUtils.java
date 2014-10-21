package jcbp.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class CardUtils {

	/**
	 * Helper function to recursively delete a file or directory
	 * @param file
	 * @throws FileNotFoundException
	 */
	public static void delete(File file) throws FileNotFoundException {
		if(file.exists()) {
			if (file.isDirectory()) {
				for (File c : file.listFiles()) {
					delete(c);
				}
			}
			if (!file.delete()){
				throw new FileNotFoundException("Failed to delete file: " + file);
			}
		}
	}
	
	/**
	 * Creates a temp directory
	 * @return
	 * @throws IOException
	 */
	public static File createTempDirectory() throws IOException {
		final File temp = File.createTempFile("temp", Long.toString(System.nanoTime()));
		if (!temp.delete()) {
			throw new IOException("Could not delete temp file: " + temp.getAbsolutePath());
		}
		if (!temp.mkdir()) {
			throw new IOException("Could not create temp directory: " + temp.getAbsolutePath());
		}
		return temp;
	}

	/**
	 * Unjars (unzips) a jar file
	 * @param jarFile
	 * @param outputDirectory
	 * @throws IOException
	 */
	public static void unjar(File jarFile, File outputDirectory) throws IOException{
		unjar(jarFile.getAbsolutePath(), outputDirectory.getAbsolutePath());
	}
	
	// helper file to unzip the WAR file contents
	private static void unjar(String zipFile, String outputDirectory) throws IOException {
		byte[] buffer = new byte[1024];
		ZipFile archive = new ZipFile(new File(zipFile));
		try {
			Enumeration<? extends ZipEntry> entries = archive.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				File file = new File(outputDirectory + File.separator + entry.getName());
				// create a place to put the file or make the directory
				if(entry.isDirectory()){
					file.mkdirs();
					continue;
				} else {
					new File(file.getParent()).mkdirs();
				}
				// write the file
				FileOutputStream out = new FileOutputStream(file);
				try {
					InputStream in = archive.getInputStream(entry);
					try {
						int len;
						while ((len = in.read(buffer)) > 0) {
							out.write(buffer, 0, len);
						}
					} finally {
						in.close();
					}
					out.flush();
				} finally {
					out.close();
				}
			}
		} finally {
			archive.close();
		}
	}
	
	/**
	 * Helper method for zipping a directory
	 * @param directoryToJar
	 * @param outputZipFile
	 * @return
	 */
	public static boolean jarDirectory(File directoryToJar, File outputZipFile){
		ZipHelper zip = new ZipHelper();
        try {
        	zip.zipDir(directoryToJar, outputZipFile);
        } catch(IOException e) {
            return false;
        }
        return true;
	}
	
	/**
	 * Helper method to jar (zip) a directory
	 * @param directory
	 * @param outputJar
	 * @return
	 */
	public static boolean jar(File directory, File outputJar){
		ZipHelper zip = new ZipHelper();
        try {
        	zip.zipDir(directory, outputJar);
        } catch(IOException e) {
            return false;
        }
        return true;
	}

	/**
	 * Stolen and modified from: http://stackoverflow.com/a/15969656/475329
	 */
	private static class ZipHelper  {
	    public void zipDir(File directory, File outputFile) throws IOException {
	        FileOutputStream fw = new FileOutputStream(outputFile);
	        ZipOutputStream zip = new ZipOutputStream(fw);
	        for(File f : directory.listFiles()){
        		if(f.isDirectory()){
        			addFolderToZip("", f, zip);
        		} else {
        			addFileToZip("", f, zip, false);
        		}
        	}
	        
	        zip.close();
	        fw.close();
	    }

	    private void addFolderToZip(String path, File srcFolder, ZipOutputStream zip) throws IOException {
	        if (srcFolder.list().length == 0) {
	            addFileToZip(path , srcFolder, zip, true);
	        }
	        else {
	            for (String fileName : srcFolder.list()) {
	                if (path.equals("")) {
	                    addFileToZip(srcFolder.getName(), new File(srcFolder + "/" + fileName), zip, false);
	                } 
	                else {
	                     addFileToZip(path + "/" + srcFolder.getName(), new File(srcFolder.getAbsolutePath() + "/" + fileName), zip, false);
	                }
	            }
	        }
	    }

	    private void addFileToZip(String path, File srcFile, ZipOutputStream zip, boolean flag) throws IOException {
	        if (flag) {
	            zip.putNextEntry(new ZipEntry(path + "/" +srcFile.getName() + "/"));
	        }
	        else {
	            if (srcFile.isDirectory()) {
	                addFolderToZip(path, srcFile, zip);
	            }
	            else {
	                byte[] buf = new byte[1024];
	                int len;
	                FileInputStream in = new FileInputStream(srcFile);
	                zip.putNextEntry(new ZipEntry(path + "/" + srcFile.getName()));
	                while ((len = in.read(buf)) > 0) {
	                    zip.write(buf, 0, len);
	                }
	                in.close();
	            }
	        }
	    }
	}
	
}
