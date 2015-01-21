package org.container.directory;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * This element filter filters files from the Java Package View if they are
 * included in a SimpleDirContainer that is on the parent Java project's
 * classpath. This will prevent the user from right-clicking hte file and adding
 * it to the build path as a CPE_LIBRARY classpath entry and thus prevent
 * duplication on the classpath.
 * 
 * @author Aaron J Tarter
 */
public class ContainerDirFilter extends ViewerFilter {

   /*
    * @ return false if the Java element is a file that is contained in a
    * SimpleDirContainer that is in the classpath of the owning Java project
    * (non-Javadoc)
    * 
    * @see org.eclipse.jface.viewers.ViewerFilter#select(
    * org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
    */
   @Override
   public boolean select(Viewer viewer, Object parentElement, Object element) {
      if (element instanceof IFile) {
         IFile f = (IFile) element;
         IJavaProject jp = JavaCore.create(f.getProject());
         try {
            // lets see if this file is included in a SimpleDirContainer
            IClasspathEntry[] entries = jp.getRawClasspath();
            for (IClasspathEntry entry : entries) {
               if (entry.getEntryKind() == IClasspathEntry.CPE_CONTAINER) {
                  if (SimpleDirContainer.ID.isPrefixOf(entry.getPath())) {
                     // we know this is a SimpleDirContainer so lets get the instance
                     SimpleDirContainer con = (SimpleDirContainer) JavaCore.getClasspathContainer(entry.getPath(), jp);
                     if (con.isFileExtensionContained(f.getLocation().toFile())) {
                        // this file will is included in the container, so dont show it
                        return false;
                     }
                  }
               }
            }
         } catch (JavaModelException e) {
            Logger.log(Logger.ERROR, e);
         }
      }
      return true;
   }

}
