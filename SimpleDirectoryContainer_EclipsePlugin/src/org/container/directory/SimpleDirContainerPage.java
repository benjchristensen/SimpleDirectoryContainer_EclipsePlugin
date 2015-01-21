package org.container.directory;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPage;
import org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

/**
 * This classpath container page colects the directory and the file extensions
 * for a new or existing SimpleDirContainer.
 * 
 * @author Aaron J Tarter
 */
public class SimpleDirContainerPage extends WizardPage implements IClasspathContainerPage, IClasspathContainerPageExtension {

   private final static String DEFAULT_EXTS = "jar,zip";

   private IJavaProject _proj;
   private IClasspathEntry[] _classpathEntries;

   private Combo _dirCombo;
   private Button _dirBrowseButton;
   private Text _extText;
   private IPath _initPath = null;
   private Composite composite;

   /**
    * Default Constructor - sets title, page name, description
    */
   public SimpleDirContainerPage() {
      super(Messages.PageName, Messages.PageTitle, null);
      setDescription(Messages.PageDesc);
      setPageComplete(true);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jdt.ui.wizards.IClasspathContainerPageExtension#initialize
    * (org.eclipse.jdt.core.IJavaProject,
    * org.eclipse.jdt.core.IClasspathEntry[])
    */
   public void initialize(IJavaProject project, IClasspathEntry[] currentEntries) {
      _proj = project;
      _classpathEntries = currentEntries;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets
    * .Composite)
    */
   public void createControl(Composite parent) {
      composite = new Composite(parent, SWT.NULL);
      GridLayout gl_composite = new GridLayout();
      gl_composite.numColumns = 4;
      composite.setLayout(gl_composite);
      composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
      composite.setFont(parent.getFont());

      createDirGroup(composite);

      createExtGroup(composite);

      setControl(composite);

      Label label = new Label(composite, SWT.NONE);
      label.setText(Messages.DirLabel);

      _dirCombo = new Combo(composite, SWT.SINGLE | SWT.BORDER);
      GridData gd__dirCombo = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
      gd__dirCombo.widthHint = 307;
      _dirCombo.setLayoutData(gd__dirCombo);
      _dirCombo.setText(getInitDir());

      _dirBrowseButton = new Button(composite, SWT.PUSH);
      _dirBrowseButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
      _dirBrowseButton.setText(Messages.Browse);
      _dirBrowseButton.addSelectionListener(new SelectionAdapter() {
         public void widgetSelected(SelectionEvent e) {
            handleDirBrowseButtonPressed();
         }
      });

      Label label_1 = new Label(composite, SWT.NONE);
      label_1.setText(Messages.ExtLabel);

      _extText = new Text(composite, SWT.BORDER);
      _extText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
      _extText.setText(getInitExts());
      new Label(composite, SWT.NONE);
      new Label(composite, SWT.NONE);
   }

   /**
    * Creates the directory label, combo, and browse button
    * 
    * @param parent
    *           the parent widget
    */
   private void createDirGroup(Composite parent) {
   }

   /**
    * Creates the extensions label and text box
    * 
    * @param parent
    *           parent widget
    */
   private void createExtGroup(Composite parent) {
   }

   /**
    * Creates a directory dialog
    */
   protected void handleDirBrowseButtonPressed() {
      DirectoryDialog dialog = new DirectoryDialog(getContainer().getShell(), SWT.SAVE);
      dialog.setMessage(Messages.DirSelect);
      dialog.setFilterPath(getLibDirectoryValue());
      String dir = dialog.open();
      if (dir != null) {
         _dirCombo.setText(dir);
      }
   }

   /**
    * Extracts the initial directory value from a path passed in setSelection()
    * 
    * @return the inital directory value
    */
   private String getInitDir() {
      String result;
      
      result = SimpleDirContainer.getFullPath(_initPath, _proj);

      return result;
   }

   /**
    * Extracts the initial extensions list from a path passed in setSelection()
    * 
    * @return the intial comma separated list of extensions
    */
   private String getInitExts() {
      String result = null;
      
      IPath defaultPath;
      if (_initPath == null) {
         // we have no entry and we are creating now a new one, so provide a default initPath
         defaultPath = _proj.getPath();
      } else {
         defaultPath = _initPath;
      }
      
      SimpleDirContainer found = SimpleDirContainer.lookupExistingContainer(defaultPath, _proj); 
      if (found == null) {
         result = DEFAULT_EXTS;
      } else {
         String exts = SimpleDirContainer.getExtraAttributeValue(found, ClasspathExtraAttribute.FILE_EXTENSTIONS);
         if (exts == null) {
            result = DEFAULT_EXTS;
         } else {
            result = exts;
         }
      }
      
      return result;
   }

   /**
    * @return the current extension list
    */
   protected String getExtensionValue() {
      return _extText.getText().trim();
   }

   /**
    * @return the current directory
    */
   protected String getLibDirectoryValue() {
      return _dirCombo.getText();
   }

   /**
    * @return directory relative to the parent project
    */
   protected String getRelativeDirValue() {
      int projDirLen = _proj.getProject().getLocation().toString().length();
      return getLibDirectoryValue().substring(projDirLen);
   }

   /**
    * Checks that the directory is a subdirectory of the project being
    * configured
    * 
    * @param dir
    *           a directory to validate
    * @return true if the directory is valid
    */
   private boolean isDirValid(String dir) {
      Path dirPath = new Path(dir);
      return _proj.getProject().getLocation().makeAbsolute().isPrefixOf(dirPath);
   }

   /**
    * Checks that the list of comma separated extensions are valid. Must meet
    * the following: - non-null and non-empty - match the regex [a-z_][a-z_,]*
    * 
    * @param exts
    *           comma separated list of extensions
    * @return true if the extension list is valid
    */
   private boolean areExtsValid(String exts) {
      if (exts == null || exts.equals("")) {
         return false;
      }
      // else
      return exts.matches("[a-z_][a-z_,]*");
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#finish()
    */
   public boolean finish() {
      if (!areExtsValid(getExtensionValue())) {
         setErrorMessage(Messages.ExtErr);
         return false;
      } else if (!isDirValid(getLibDirectoryValue())) {
         setErrorMessage(NLS.bind(Messages.DirErr, _proj.getProject().getName()));
         return false;
      }
      return true;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.ui.wizards.IClasspathContainerPage#getSelection()
    */
   public IClasspathEntry getSelection() {
      String dir = getRelativeDirValue();
      
      IClasspathEntry entry = SimpleDirContainer.getClasspathEntry(_proj, dir, getExtensionValue());
      
      return entry;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jdt.ui.wizards.IClasspathContainerPage#setSelection(org.eclipse
    * .jdt.core.IClasspathEntry)
    */
   public void setSelection(IClasspathEntry containerEntry) {
      if (containerEntry != null) {
         _initPath = containerEntry.getPath();
      }
   }
}
