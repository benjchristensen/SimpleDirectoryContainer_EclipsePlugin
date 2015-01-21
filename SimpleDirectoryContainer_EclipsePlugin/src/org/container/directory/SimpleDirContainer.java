package org.container.directory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.ClasspathAttribute;
import org.eclipse.jdt.internal.core.ClasspathEntry;

/**
 * * This classpath container add archive files from a configured project
 * directory to the classpath as CPE_LIBRARY entries, and it attaches -src
 * archives as source attachments.
 * 
 * @author Aaron J Tarter
 * @author benjchristensen (modified handling of filenames with multiple .'s in
 *         them)
 * @author Darko Palic
 */
public class SimpleDirContainer implements IClasspathContainer {
   public final static Path ID = new Path("org.container.directory.SIMPLE_DIR_CONTAINER");
   // use this string to represent the root project directory
   public final static String ROOT_DIR = "-";

   // user-fiendly name for the container that shows on the UI
   private String _desc;

   // Project of this path
   private IJavaProject _project;
   // path string that uniquiely identifies this container instance
   private IPath _path;
   // directory that will hold files for inclusion in this container
   private File _dir;
   // Filename extensions to include in container
   private HashSet<String> _exts;

   private IClasspathEntry[] classpathEntries;

   private Status currentStatus = Status.Initializing;

   /**
    * Define the current status of this instance.
    * 
    * @author Darko Palic
    */
   private enum Status {
      Initializing, InUse, RequestedContainerUpdate
   }

   /**
    * This filename filter will be used to determine which files will be
    * included in the container
    */
   private FilenameFilter _dirFilter = new FilenameFilter() {

      /**
       * This File filter is used to filter files that are not in the configured
       * extension set. Also, filters out files that have the correct extension
       * but end in -src, since filenames with this pattern will be attached as
       * source.
       * 
       * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
       */
      public boolean accept(File dir, String name) {
         // if there is no dot then we don't have an extension and we'll skip
         // this
         if (name.lastIndexOf('.') == -1) {
            return false;
         }

         String ext = name.substring(name.lastIndexOf('.') + 1, name.length()).toLowerCase();
         String prefix = name.substring(0, name.lastIndexOf('.'));

         // lets avoid including filnames that end with -src since
         // we will use this as the convention for attaching source
         if (prefix.endsWith("-src")) {
            return false;
         }
         // Darko TODO: handling of dir container extension must be improved at
         // all. Just a hack for the previous code
         if (_exts == null) {
            // Logger.log(Logger.WARNING,
            // "Directory Container could not read the extensions from .classpath for: "
            // + dir.toString()
            // + " of name " + name);
            return true;
         } else {
            if (_exts.contains(ext)) {
               return true;
            }
         }

         return false;
      }
   };

   /**
    * This constructor uses the provided IPath and IJavaProject arguments to
    * assign the instance variables that are used for determining the classpath
    * entries included in this container. The provided IPath comes from the
    * classpath entry element in project's .classpath file. It is a three
    * segment path with the following segments: [0] - Unique container ID [1] -
    * project relative directory that this container will collect files from [2]
    * - comma separated list of extensions to include in this container
    * (extensions do not include the preceding ".")
    * 
    * @param containerPath
    *           unique path for this container instance, including directory and
    *           extensions a segments
    * @param project
    *           the Java project that is referencing this container
    */
   public SimpleDirContainer(final IPath containerPath, IJavaProject project) {
      _path = containerPath;
      _project = project;

      // // extract the extension types for this container from the path
      // String extString = path.lastSegment();
      // _exts = new HashSet<String>();
      // String[] extArray = extString.split(",");
      // for (String ext : extArray) {
      // _exts.add(ext.toLowerCase());
      // }

      File classPathFolder = getClasspathDirectory(containerPath, project);
      _dir = classPathFolder;

      // Create UI String for this container that reflects the directory being
      // used
      _desc = createDescription(containerPath, project);

      classpathEntries = resolveLibsFromDir(_dir);

      currentStatus = Status.InUse;
   }

   /**
    * extract the directory string from the PATH and create the directory
    * relative to the project.
    * 
    * @param containerPath
    *           the full path of the container
    * @param project
    *           the container project
    * @return the file reference to the dir
    */
   private File getClasspathDirectory(final IPath containerPath, IJavaProject project) {
      IPath libsPath = createDescriptionPath(containerPath, project);
      File rootProj = project.getProject().getLocation().makeAbsolute().toFile();
      // remove indicator path of project
      IPath relativePath = libsPath.removeFirstSegments(1);
      File classPathFolder = new File(rootProj, relativePath.toString());
      if (!classPathFolder.exists()) {
         Logger.log(Logger.WARNING,
               "Folder of Directory Container missing: " + libsPath.toString() + " of project " + project.getElementName()
                     + " creating it now to avoid errors in eclipse");

         classPathFolder.mkdirs();
      }
      return classPathFolder;
   }

   /**
    * This method is used to determine if the directory specified in the
    * container path is valid, i.e. it exists relative to the project and it is
    * a directory.
    * 
    * @return true if the configured directory is valid
    */
   public boolean isValid() {
      if (_dir.exists() && _dir.isDirectory()) {
         return true;
      }
      return false;
   }

   /**
    * Returns a set of CPE_LIBRARY entries from the configured project directory
    * that conform to the configured set of file extensions and attaches a
    * source archive to the libraries entries if a file with same name ending
    * with -src is found in the directory.
    * 
    * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
    */
   public IClasspathEntry[] getClasspathEntries() {
      if (isClasspathChanged() && (currentStatus != Status.RequestedContainerUpdate)) {
         try {
            // okay fine, we have an change in the classpath, so request an update to eclipse
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            IProject project = root.getProject(_project.getElementName());
            IJavaProject javaProject = JavaCore.create(project);

            // a very nice info, which is completly missing in the eclipse docs.
            // http://stackoverflow.com/questions/12806888/refreshing-classpath-container-name-in-eclipse
            // means this class may never change, so we will update the container and
            // set a new one.
            // The container must be immutable, so we have to request a update
            ClasspathContainerInitializer initializer = JavaCore.getClasspathContainerInitializer(ID.toString());
            initializer.requestClasspathContainerUpdate(_path, _project, new SimpleDirContainer(_path, javaProject));

            currentStatus = Status.RequestedContainerUpdate;
         } catch (CoreException e) {
            Logger.log(Logger.ERROR, "getClasspathEntries - ERROR: " + e.getMessage());
         }
      }
      
      return classpathEntries;
   }

   /**
    * TODO: this is a evil hack, should be refactored properly to do only small steps.
    * 
    * @param classpathDirectory the directory of the given classpath folder in eclipse.
    * @return the array of the classpath entries in the given folder
    */
   private IClasspathEntry[] resolveLibsFromDir(File classpathDirectory) {
      ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
      // fetch the names of all files that match our filter

      File resolvedDir = classpathDirectory;
      if (resolvedDir.isDirectory()) {

         // get the files and sort the files to be in correct lexical order in eclipse classpath later
         File[] libs = resolvedDir.listFiles(_dirFilter);
         Arrays.sort(libs);

         try {
            for (File lib : libs) {
               // strip off the file extension
               String[] splittedName = lib.getName().split("[.]");
               String ext = splittedName[splittedName.length - 1];

               // TODO: this is really a awful check against sources
               // now see if this archive has an associated src jar
               File srcArc = new File(lib.getAbsolutePath().replace("." + ext, "-src." + ext));
               Path srcPath = null;
               // if the source archive exists then get the path to attach it
               if (srcArc.exists()) {
                  srcPath = new Path(srcArc.getAbsolutePath());
               }
               // create a new CPE_LIBRARY type of cp entry with an attached
               // source
               // archive if it exists

               IClasspathEntry entry = JavaCore.newLibraryEntry(new Path(lib.getAbsolutePath()), srcPath, new Path("/"));
               entryList.add(entry);
            }
         } catch (Exception e) {
            e.printStackTrace();
            Logger.log(Logger.ERROR, "getClasspathEntries - ERROR: " + e.getMessage());
         }
      }
      
      
      // convert the list to an array and return it
      IClasspathEntry[] entryArray = new IClasspathEntry[entryList.size()];

      return (IClasspathEntry[]) entryList.toArray(entryArray);
   }

   private boolean isClasspathChanged() {
      boolean result = false;
      
      List<IClasspathEntry> current = Arrays.asList(classpathEntries);
      IClasspathEntry[] newlyResolved = resolveLibsFromDir(_dir);
      List<IClasspathEntry> difference = new ArrayList<IClasspathEntry>();
      difference.addAll(current);
      difference.removeAll(Arrays.asList(newlyResolved));
            
      if (newlyResolved.length != classpathEntries.length) {
         // okay trivial test
         result = true;
      } else if (difference.size() > 0) {
         // maybe same mount of references reside, but the libs have changed (e.g. filename, version, ...)
         result = true;
      }
      return result;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.core.IClasspathContainer#getDescription()
    */
   public String getDescription() {
      return _desc;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.core.IClasspathContainer#getKind()
    */
   public int getKind() {
      return IClasspathContainer.K_APPLICATION;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.core.IClasspathContainer#getPath()
    */
   public IPath getPath() {
      return _path;
   }

   /*
    * @return configured directory for this container
    */
   public File getDir() {
      return _dir;
   }

   /*
    * @return whether or not this container would include the file
    */
   public boolean isFileExtensionContained(File file) {
      if (file.getParentFile().equals(_dir)) {
         // peel off file extension
         String fExt = file.toString().substring(file.toString().lastIndexOf('.') + 1);
         // check is it is in the set of cofigured extensions
         if ((_exts != null) && (_exts.contains(fExt.toLowerCase()))) {
            return true;
         }
      }
      return false;
   }

   /**
    * Get the full path of the the given path and project.
    * 
    * @param path
    *           the path
    * @param project
    *           the java project
    * @return the full path to this classpathContainer
    */
   public static String getFullPath(IPath path, IJavaProject project) {
      String result;

      String projDir = project.getProject().getLocation().toString();

      if (path != null && !path.makeRelativeTo(SimpleDirContainer.ID).isEmpty()) {
         result = projDir + IPath.SEPARATOR + path.makeRelativeTo(SimpleDirContainer.ID);
      } else {
         result = projDir;
      }

      return result;

   }

   public static IClasspathEntry getClasspathEntry(IJavaProject project, String projectRelativeLibFolder, String fileExtensions) {
      String dir = projectRelativeLibFolder;
      if (dir.equals("")) {
         dir = SimpleDirContainer.ROOT_DIR;
      }
      IPath containerPath = SimpleDirContainer.ID.append("/" + project.getElementName() + "/" + dir);

      Set<IClasspathAttribute> attrList = new HashSet<IClasspathAttribute>();
      attrList.addAll(Arrays.asList(ClasspathEntry.NO_EXTRA_ATTRIBUTES));
      ClasspathAttribute extensions = new ClasspathAttribute(ClasspathExtraAttribute.FILE_EXTENSTIONS.getValue(), fileExtensions);
      attrList.add(extensions);
      IClasspathAttribute[] attributes = new IClasspathAttribute[attrList.size()];
      attrList.toArray(attributes);

      IClasspathEntry entry = JavaCore.newContainerEntry(containerPath, ClasspathEntry.NO_ACCESS_RULES, attributes, false);

      return entry;
   }

   public static String getExtraAttributeValue(SimpleDirContainer simpleDirContainer, ClasspathExtraAttribute attribute) {
      String result = null;
      if (simpleDirContainer.getClasspathEntries().length > 0) {
         for (IClasspathAttribute attr : simpleDirContainer.getClasspathEntries()[0].getExtraAttributes()) {
            if (attr.getName().equalsIgnoreCase(attribute.getValue())) {
               result = attr.getValue();
            }
         }
      }
      return result;
   }

   /**
    * Lookup in eclipse project settings the corresponding ClasspathContainer.
    * 
    * @param containerPath
    *           the containerpath of SimpleDirContainer
    * @param project
    *           the project there the container is registered
    * @return null or the found container
    */
   public static SimpleDirContainer lookupExistingContainer(IPath containerPath, IJavaProject project) {
      SimpleDirContainer result = null;

      try {
         IClasspathContainer storedContainer = JavaCore.getClasspathContainer(containerPath, project);

         if (storedContainer != null) {
            IPath pluginID = storedContainer.getPath().removeLastSegments(storedContainer.getPath().segmentCount() - 1);
            String storedPath = storedContainer.getPath().toString();
            // IPath prj = containerPath.removeFirstSegments(1);
            // prj = prj.removeLastSegments(prj.segmentCount() - 1);
            // String storedProjectName = prj.toString();

            if (storedContainer instanceof SimpleDirContainer) {
               result = (SimpleDirContainer) storedContainer;
            } else if ((storedContainer instanceof IClasspathContainer) && (pluginID.equals(ID)) &&
            // Darko TODO: fix this really awful hack
                  (storedContainer.getClass().getSimpleName().equals("PersistedClasspathContainer"))) {
               result = new SimpleDirContainer(storedContainer.getPath(), project);
            } else {
               Logger.log(Logger.ERROR, "Classpath container " + SimpleDirContainer.class.getSimpleName() + " lookup failed: "
                     + containerPath.toString() + " of project " + project.getElementName()
                     + " found a corresponding path-value to SimpleDirContainer but not a instance of it");
            }
         }
      } catch (JavaModelException modelEx) {
         Logger.log(Logger.ERROR,
               "Classpath container " + SimpleDirContainer.class.getSimpleName() + " lookup failed: " + containerPath.toString()
                     + " of project " + project.getElementName());
      }

      return result;
   }

   private static IPath createDescriptionPath(IPath containerPath, IJavaProject project) {
      IPath result = containerPath.removeFirstSegments(1);
      if (result.segmentCount() == 1 && result.segment(0).equals(ROOT_DIR)) {
         result = result.removeFirstSegments(1);
      }
      return result;
   }

   public static String createDescription(IPath containerPath, IJavaProject project) {
      String result = null;

      result = "Directory Classpath: /" + createDescriptionPath(containerPath, project);

      return result;
   }

   @Override
   public String toString() {
      return "SimpleDirContainer [_desc=" + _desc + ", _project=" + _project + ", _path=" + _path + ", _dir=" + _dir + ", _exts=" + _exts
            + ", _dirFilter=" + _dirFilter + "]";
   }

}
