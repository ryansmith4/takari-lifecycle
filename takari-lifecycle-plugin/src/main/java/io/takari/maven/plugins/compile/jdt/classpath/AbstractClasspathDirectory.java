package io.takari.maven.plugins.compile.jdt.classpath;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleException;

import com.google.common.collect.ImmutableMap;

abstract class AbstractClasspathDirectory extends DependencyClasspathEntry implements ClasspathEntry {

  private final Map<String, Path> files;

  protected AbstractClasspathDirectory(Path directory, Set<String> packages, Map<String, Path> files) {
    super(directory, packages, getExportedPackages(directory));
    this.files = ImmutableMap.copyOf(files);
  }

  protected static void scanDirectory(Path basedir, String suffix, Set<String> packages, Map<String, Path> files) {
    try {
      Files.walkFileTree(basedir, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
          String relpath = basedir.relativize(dir).toString();
          if (!relpath.isEmpty()) {
            packages.add(relpath.replace('\\', '/'));
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          String relpath = basedir.relativize(file).toString();
          if (relpath.endsWith(suffix)) {
            files.put(relpath.substring(0, relpath.length() - suffix.length()).replace('\\', '/'), file);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (NoSuchFileException expected) {
      // the directory does not exist, nothing to be alarmed about
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Collection<String> getExportedPackages(Path directory) {
    Collection<String> exportedPackages = null;
    try (InputStream is = Files.newInputStream(directory.resolve(PATH_EXPORT_PACKAGE))) {
      exportedPackages = parseExportPackage(is);
    } catch (IOException e) {
      // silently ignore missing/bad export-package files
    }
    if (exportedPackages == null) {
      try (InputStream is = Files.newInputStream(directory.resolve(PATH_MANIFESTMF))) {
        exportedPackages = parseBundleManifest(is);
      } catch (IOException | BundleException e) {
        // silently ignore missing/bad export-package files
      }
    }
    return exportedPackages;
  }

  public Path getFile(String packageName, String typeName) {
    String qualifiedFileName = packageName + "/" + typeName;
    return files.get(qualifiedFileName);
  }

}
