package jcbp.ui;

import jcbp.Activator;

import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String JAVACARD_SDK_PATH = "JAVACARD_SDK_PATH";
	public static final String JAVACARD_SDK_PATH_DESCRIPTION = "Java Card SDK Directory";
	
	public PreferencePage() {
		super(GRID);
	}

	@Override
	public void init(IWorkbench workbench) {
		setPreferenceStore(Activator.getDefault().getPreferenceStore());
		setDescription("Configure preferences for the WAR Binary Processing plugin.");
	}

	@Override
	protected void createFieldEditors() {
		addField(new DirectoryFieldEditor(JAVACARD_SDK_PATH, 
				"&" + JAVACARD_SDK_PATH_DESCRIPTION + ":", getFieldEditorParent()));
	}

}
