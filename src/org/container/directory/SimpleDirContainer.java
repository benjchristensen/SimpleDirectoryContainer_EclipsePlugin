package org.container.directory;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * * This classpath container add archive files from a configured project
 * directory to the classpath as CPE_LIBRARY entries, and it attaches -src
 * archives as source attachments.
 * 
 * @author Aaron J Tarter
 * @author benjchristensen (modified handling of filenames with multiple .'s in them)
 */
public class SimpleDirContainer implements IClasspathContainer {
    public final static Path ID = new Path(
            "org.container.directory.SIMPLE_DIR_CONTAINER");
    // use this string to represent the root project directory
    public final static String ROOT_DIR = "-";

    // user-fiendly name for the container that shows on the UI
    private String _desc;
    // path string that uniquiely identifies this container instance
    private IPath _path;
    // directory that will hold files for inclusion in this container
    private File _dir;
    // Filename extensions to include in container
    private HashSet<String> _exts;

    /**
     * This filename filter will be used to determine which files will be
     * included in the container
     */
    private FilenameFilter _dirFilter = new FilenameFilter() {

        /**
         * This File filter is used to filter files that are not in the
         * configured extension set. Also, filters out files that have the
         * correct extension but end in -src, since filenames with this pattern
         * will be attached as source.
         * 
         * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
         */
        public boolean accept(File dir, String name) {
            // if there is no dot then we don't have an extension and we'll skip this
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
            if (_exts.contains(ext)) {
                return true;
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
     * project relative directory that this container will collect files from
     * [2] - comma separated list of extensions to include in this container
     * (extensions do not include the preceding ".")
     * 
     * @param path
     *            unique path for this container instance, including directory
     *            and extensions a segments
     * @param project
     *            the Java project that is referencing this container
     */
    public SimpleDirContainer(IPath path, IJavaProject project) {
        _path = path;

        // extract the extension types for this container from the path
        String extString = path.lastSegment();
        _exts = new HashSet<String>();
        String[] extArray = extString.split(",");
        for (String ext : extArray) {
            _exts.add(ext.toLowerCase());
        }
        // extract the directory string from the PATH and create the directory
        // relative
        // to the project
        path = path.removeLastSegments(1).removeFirstSegments(1);
        File rootProj = project.getProject().getLocation().makeAbsolute().toFile();
        if (path.segmentCount() == 1 && path.segment(0).equals(ROOT_DIR)) {
            _dir = rootProj;
            path = path.removeFirstSegments(1);
        } else {
            _dir = new File(rootProj, path.toString());
        }

        // Create UI String for this container that reflects the directory being used
        _desc = "Directory Classpath: /" + path;

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
     * Returns a set of CPE_LIBRARY entries from the configured project
     * directory that conform to the configured set of file extensions and
     * attaches a source archive to the libraries entries if a file with same
     * name ending with -src is found in the directory.
     * 
     * @see org.eclipse.jdt.core.IClasspathContainer#getClasspathEntries()
     */
    public IClasspathEntry[] getClasspathEntries() {
        ArrayList<IClasspathEntry> entryList = new ArrayList<IClasspathEntry>();
        // fetch the names of all files that match our filter
        File[] libs = _dir.listFiles(_dirFilter);

        try {
            for (File lib : libs) {
                // strip off the file extension
                String ext = lib.getName().split("[.]")[1];

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

                entryList
                        .add(JavaCore.newLibraryEntry(
                                new Path(lib.getAbsolutePath()), srcPath,
                                new Path("/")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Logger.log(Logger.ERROR, "getClasspathEntries - ERROR: " + e.getMessage());
        }
        // convert the list to an array and return it
        IClasspathEntry[] entryArray = new IClasspathEntry[entryList.size()];

        return (IClasspathEntry[]) entryList.toArray(entryArray);
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
    public boolean isContained(File file) {
        if (file.getParentFile().equals(_dir)) {
            // peel off file extension
            String fExt = file.toString().substring(
                    file.toString().lastIndexOf('.') + 1);
            // check is it is in the set of cofigured extensions
            if (_exts.contains(fExt.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
