package jcbp.ui;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import jcbp.Activator;
import jcbp.common.CardToJimple;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.WizardNewProjectCreationPage;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;

import com.ensoftcorp.atlas.core.log.Log;

public class ImportCAPWizard extends Wizard implements IImportWizard {

	private NewCAPBinaryProjectPage page;
	
	public ImportCAPWizard(String startCAPPath, String startEXPPath) {
		page = new NewCAPBinaryProjectPage("Create Java Card Binary Project", startCAPPath, startEXPPath);
		String projectName = new File(startCAPPath).getName();
		projectName = projectName.substring(0, projectName.lastIndexOf('.'));
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (project.exists()) {
			// find a project name that doesn't collide
			int i = 2;
			while (project.exists()) {
				i++;
				project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName + "_" + i);
			}
			projectName = projectName + "_" + i;
		}
		page.setInitialProjectName(projectName);
		this.setWindowTitle("Create Java Card Binary Project");
	}
	
	public ImportCAPWizard() {
		page = new NewCAPBinaryProjectPage("Create Java Card Binary Project");
		this.setWindowTitle("Create Java Card Binary Project");
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {}
	
	@Override
	public void addPages() {
		this.addPage(page);
	}

	@Override
	public boolean performFinish() {
		final String projectName = page.getProjectName();
		final IPath projectLocation = page.getLocationPath();
		final File capFile = new File(page.getCAPPath());
		final File expFile = new File(page.getEXPPath());

		IRunnableWithProgress j = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) {
				IStatus result = null;
				try {
					result = CardToJimple.createJavaCardBinaryProject(projectName, projectLocation, capFile, expFile, monitor);
				} catch (Throwable t) {
					String message = "Could not create Java Card binary project. " + t.getMessage();
					UIJob uiJob = new ShowErrorDialogJob("Showing error dialog", message, projectName);
					uiJob.schedule();
					Log.error(message, t);
				} finally {
					monitor.done();
				}
				if(result.equals(Status.CANCEL_STATUS)) {
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					deleteProject(project);
				}
			}
		};

		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		
		try {
			dialog.run(true, true, j);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public static IStatus deleteProject(IProject project) {
		if (project != null && project.exists())
			try {
				project.delete(true, true, new NullProgressMonitor());
			} catch (CoreException e) {
				Log.error("Could not delete project", e);
				return new Status(Status.ERROR, Activator.PLUGIN_ID, "Could not delete project", e);
			}
		return Status.OK_STATUS;
	}
	
	private static class NewCAPBinaryProjectPage extends WizardNewProjectCreationPage {
		private String capPath;
		private String expPath;
		
		public NewCAPBinaryProjectPage(String pageName, String startCAPPath, String startEXPPath) {
			super(pageName);
			capPath = startCAPPath;
			expPath = startEXPPath;
		}

		public NewCAPBinaryProjectPage(String pageName) {
			this(pageName, "", "");
		}
		
		public String getCAPPath() {
			return capPath;
		}
		
		public String getEXPPath() {
			return expPath;
		}
		
		@Override
		public void createControl(Composite parent) {
			super.createControl(parent);
			Composite composite = (Composite) this.getControl();
			
			final FileDialog capFileChooser = new FileDialog(composite.getShell(), SWT.OPEN);
			capFileChooser.setFilterExtensions(new String[] { "*.cap" });
			
			final FileDialog expFileChooser = new FileDialog(composite.getShell(), SWT.OPEN);
			expFileChooser.setFilterExtensions(new String[] { "*.exp" });
			
			Composite row = new Composite(composite, SWT.NONE);
			GridLayout layout = new GridLayout();
			layout.numColumns = 3;
			row.setLayout(layout);
			
			GridData data = new GridData();
			data.horizontalAlignment = SWT.FILL;
			row.setLayoutData(data);
			
			Label labelCAP = new Label(row, SWT.NONE);
			labelCAP.setText("CAP:");
			
			final Text textCAP = new Text(row, SWT.SINGLE | SWT.BORDER);
			textCAP.setText(capPath);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			textCAP.setLayoutData(data);
			
			textCAP.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					capPath = textCAP.getText();
				}
			});
			
			Button buttonBrowseCAP = new Button(row, SWT.PUSH);
			buttonBrowseCAP.setText("     Browse...     ");
			buttonBrowseCAP.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!capPath.isEmpty()){
						capFileChooser.setFileName(capPath);
					}
					String path = capFileChooser.open();
					if (path != null){
						capPath = path;
					}
					textCAP.setText(capPath);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
				
			});
			
			Label labelEXP = new Label(row, SWT.NONE);
			labelEXP.setText("EXP:");
			
			final Text textEXP = new Text(row, SWT.SINGLE | SWT.BORDER);
			textEXP.setText(capPath);
			data = new GridData();
			data.grabExcessHorizontalSpace = true;
			data.horizontalAlignment = SWT.FILL;
			textEXP.setLayoutData(data);
			
			textEXP.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					capPath = textEXP.getText();
				}
			});
			
			Button buttonBrowseEXP = new Button(row, SWT.PUSH);
			buttonBrowseEXP.setText("     Browse...     ");
			buttonBrowseEXP.addSelectionListener(new SelectionListener() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (!expPath.isEmpty()){
						capFileChooser.setFileName(expPath);
					}
					String path = capFileChooser.open();
					if (path != null){
						expPath = path;
					}
					textEXP.setText(expPath);
				}

				@Override
				public void widgetDefaultSelected(SelectionEvent e) {}
				
			});
		}
	}
	
	private static class ShowErrorDialogJob extends UIJob {

		private String message, projectName;
		
		public ShowErrorDialogJob(String name, String errorMessage, String projectName) {
			super(name);
			this.message = errorMessage;
			this.projectName = projectName;
		}

		@Override
		public IStatus runInUIThread(IProgressMonitor monitor) {
			Path iconPath = new Path("icons" + File.separator + "JCBP.gif");
			Bundle bundle = Platform.getBundle(Activator.PLUGIN_ID);
			Image icon = null;
			try {
				icon = new Image(PlatformUI.getWorkbench().getDisplay(), FileLocator.find(bundle, iconPath, null).openStream());
			} catch (IOException e) {
				Log.error("JCBP.gif icon is missing.", e);
			};
			MessageDialog dialog = new MessageDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
													"Could Not Create Java Card Binary Project", 
													icon, 
													message, 
													MessageDialog.ERROR,
													new String[] { "Delete Project", "Cancel" }, 
													0);
			int response = dialog.open();

			IStatus status = Status.OK_STATUS;
			if (response == 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
				status = deleteProject(project);
			}
			
			if (icon != null){
				icon.dispose();
			}
			
			return status;
		}
		
	}
}
