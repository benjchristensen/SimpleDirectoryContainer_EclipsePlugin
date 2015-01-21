package org.container.directory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

/**
 * This classpath container initializer constructs a SimpleDirContainer with the
 * give container path and Java project. It then validates the container before
 * setting it in the classpath. If the container is invalid, it fails silently
 * and logs an error to the Eclipse error log.
 * 
 * @author Aaron J Tarter
 */
public class SimpleDirContainerInitializer extends ClasspathContainerInitializer {

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jdt.core.ClasspathContainerInitializer#initialize(org.eclipse
    * .core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
    */
   @Override
   public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
      SimpleDirContainer container = null;
      // Darko TODO: unsure about this code to read from storage, since the lifecycle of the containers is not clear defined. 
//      if (project.isOpen()) {
//         container = SimpleDirContainer.lookupExistingContainer(containerPath, project);
//      }
  
      if (container == null) {
         container = new SimpleDirContainer(containerPath, project);
         if (container.isValid()) {
            JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { container }, null);
         } else {
            Logger.log(Logger.WARNING, Messages.InvalidContainer + containerPath);
         }
      }

   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.eclipse.jdt.core.ClasspathContainerInitializer#canUpdateClasspathContainer
    * (org.eclipse.core.runtime.IPath, org.eclipse.jdt.core.IJavaProject)
    */
   @Override
   public boolean canUpdateClasspathContainer(IPath containerPath, IJavaProject project) {
      return true;
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.eclipse.jdt.core.ClasspathContainerInitializer#
    * requestClasspathContainerUpdate(org.eclipse.core.runtime.IPath,
    * org.eclipse.jdt.core.IJavaProject,
    * org.eclipse.jdt.core.IClasspathContainer)
    */
   @Override
   public void requestClasspathContainerUpdate(IPath containerPath, IJavaProject project, IClasspathContainer containerSuggestion)
         throws CoreException {
      JavaCore
            .setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { containerSuggestion }, null);
   }

   @Override
   public Object getComparisonID(IPath containerPath, IJavaProject project) {
      IPath result = containerPath;
      // result = project.getElementName() + "/" + containerPath.toString();
      return result;
   }
   
   @Override
   public IStatus getAccessRulesStatus(IPath containerPath, IJavaProject project) {
      // TODO Auto-generated method stub
      return super.getAccessRulesStatus(containerPath, project);
   }
   
   @Override
   public String getDescription(IPath containerPath, IJavaProject project) {
      return SimpleDirContainer.createDescription(containerPath, project);
   }
   
   @Override
   public IStatus getAttributeStatus(IPath containerPath, IJavaProject project, String attributeKey) {
      // TODO Auto-generated method stub
      return super.getAttributeStatus(containerPath, project, attributeKey);
   }
   
   @Override
   public IStatus getSourceAttachmentStatus(IPath containerPath, IJavaProject project) {
      // TODO Auto-generated method stub
      return super.getSourceAttachmentStatus(containerPath, project);
   }
   
   @Override
   public IClasspathContainer getFailureContainer(IPath containerPath, IJavaProject project) {
      // TODO Auto-generated method stub
      return super.getFailureContainer(containerPath, project);
   }
   
}
