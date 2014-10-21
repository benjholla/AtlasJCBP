package jcbp.common;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jcbp.Activator;
import jcbp.ui.PreferencePage;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import soot.G;

import com.ensoftcorp.abp.common.util.JimpleUtil;
import com.ensoftcorp.abp.common.util.JimpleUtil.JimpleSource;
import com.ensoftcorp.atlas.core.log.Log;

public class CardToJimple {
	
	/**
	 * Creates an Eclipse project from CAP file
	 * General Overview:
	 * 1) Add all the jar files found in the <project>/WEB-INF directory to the classpath
	 * 2) Convert CAP file to JAR
	 * 3) Convert classes.jar to Jimple and output to <project>/jimple/*
	 */
	public static IStatus createJavaCardBinaryProject(String projectName, IPath projectPath, File capFile, File expFile, IProgressMonitor monitor) throws CoreException, IOException, SootConversionException {
		IProject project = null;
		try {
			monitor.beginTask("Creating Java Card Binary project", 3);
			monitor.setTaskName("Unpacking CAP");
			File projectDirectory = new File(projectPath + File.separator + projectName);
			
			// clean stale files from project directory
			CardUtils.delete(projectDirectory);

			// create empty Java project
			monitor.setTaskName("Creating Eclipse project");
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			URI location = null;
			if (projectPath != null){
				location = URIUtil.toURI(projectPath);
			}
			if (location != null && ResourcesPlugin.getWorkspace().getRoot().getLocationURI().equals(location)) {
				location = null;
			} else {
				location = URIUtil.toURI(URIUtil.toPath(location) + File.separator + projectName);
			}
			desc.setLocationURI(location);
			desc.setNatureIds(new String[] { JavaCore.NATURE_ID });
			
			// create and open the Eclipse project
			project.create(desc, null);
			IJavaProject jProject = JavaCore.create(project);
			project.open(new NullProgressMonitor());
			List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
			
			// TODO: add class path // add the default JVM classpath (Tomcat uses the same libraries unless it was overridden)
//			IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
//			for (LibraryLocation element : JavaRuntime.getLibraryLocations(vmInstall)) {
//				entries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
//			}
			
			// set the class path
			jProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), null);
			
			monitor.worked(1);
			Log.info("Successfully created JCBP project");
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}

			// translate CAP to bytecode
			monitor.setTaskName("Converting Java Card to Jar");
			String javacardSDKPath = Activator.getDefault().getPreferenceStore().getString(PreferencePage.JAVACARD_SDK_PATH);
			if(javacardSDKPath == null || javacardSDKPath.equals("")){
				throw new RuntimeException(PreferencePage.JAVACARD_SDK_PATH_DESCRIPTION + " is not set.");
			}
			File javacardSDKDirectory = new File(javacardSDKPath);
			if(!javacardSDKDirectory.exists()){
				throw new RuntimeException(javacardSDKDirectory.getAbsolutePath() + " does not exist.");
			}
			
			File jarFile = new File(projectDirectory.getAbsolutePath() + File.separatorChar + "classes.jar");
			try {
				Card2Jar.convertCardToJar(capFile, expFile, jarFile);
			} catch (Exception e) {
				throw new RuntimeException("Java Card to Jar conversion failed.");
			}
			
			monitor.worked(1);
			if (monitor.isCanceled()){
				return Status.CANCEL_STATUS;
			}
			
			// convert class files to jimple
			monitor.setTaskName("Converting Jar files to Jimple");
			File jimpleDirectory = new File(projectDirectory.getAbsolutePath() + File.separatorChar + "jimple");
			jarToJimple(jarFile, jimpleDirectory);
			monitor.worked(1);
			
			return Status.OK_STATUS;
		} finally {
			monitor.done();
			if (project != null && project.exists()){
				project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
		}
	}
	
	// helper method for converting a jar file of classes to jimple
	private static void jarToJimple(File jar, File outputDirectory) throws SootConversionException {
		if(!outputDirectory.exists()){
			outputDirectory.mkdirs();
		}
		G savedConfig = G.v();
		try {
			G.reset();
		
			String[] args = new String[] {
					"-src-prec", "class", 
					"--xml-attributes",
					"-f", "jimple",
					"-allow-phantom-refs",
					"-output-dir", outputDirectory.getAbsolutePath(),
					"-process-dir", jar.getAbsolutePath()
			};
			
			try {
				soot.Main.main(args);
				JimpleUtil.writeHeaderFile(JimpleSource.JAR, jar.getAbsolutePath(), outputDirectory.getAbsolutePath());
			} catch (RuntimeException e) {
				throw new SootConversionException(e);
			}
		} finally {
			G.set(savedConfig);
		}
	}
	
	// Throwable exception wrapper to make a runtime soot conversion exception checked
	private static class SootConversionException extends Exception {
		private static final long serialVersionUID = 1L;
		public SootConversionException(Throwable t) {
			super(t);
		}
	}
	
}
