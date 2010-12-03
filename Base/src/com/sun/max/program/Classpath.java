/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.max.program;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import com.sun.max.io.*;

/**
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public class Classpath {

    private static final List<Entry> EMPTY_LIST = Collections.emptyList();

    public static final Classpath EMPTY = new Classpath(EMPTY_LIST);

    private final List<Entry> entries;

    private final Map<String, ClasspathFile> classpathFileMap = new HashMap<String, ClasspathFile>();

    /**
     * An entry in a classpath is a file system path that denotes an existing {@linkplain Directory directory},
     * an existing {@linkplain Archive zip/jar} file or a {@linkplain PlainFile neither}.
     */
    public abstract static class Entry {

        /**
         * Gets the string representing the underlying path of this entry.
         */
        public final String path() {
            return file().getPath();
        }

        /**
         * Gets the File object representing the underlying path of this entry.
         */
        public abstract File file();

        /**
         * Gets the contents of a file denoted by a given path that is relative to this classpath entry. If the denoted
         * file does not exist under this classpath entry then {@code null} is returned. Any IO exception that occurs
         * when reading is silently ignored.
         *
         * @param path a file path relative to this classpath entry. This values uses the '/' character as the path
         *            separator regardless of the {@linkplain File#separatorChar default} for the underlying platform.
         */
        abstract ClasspathFile readFile(String path);

        public boolean isDirectory() {
            return false;
        }

        public boolean isArchive() {
            return false;
        }

        public boolean isPlainFile() {
            return false;
        }

        @Override
        public String toString() {
            return path();
        }

        public ZipFile zipFile() {
            return null;
        }
    }

    /**
     * Represents a classpath entry that is neither an existing directory nor an existing zip/jar archive file.
     */
    static final class PlainFile extends Entry {

        private final File file;

        public PlainFile(File file) {
            this.file = file;
        }

        @Override
        ClasspathFile readFile(String path) {
            return null;
        }

        @Override
        public File file() {
            return file;
        }

        @Override
        public boolean isPlainFile() {
            return true;
        }
    }

    /**
     * Represents a classpath entry that is a path to an existing directory.
     */
    static final class Directory extends Entry {
        private final File directory;

        public Directory(File directory) {
            this.directory = directory;
        }

        @Override
        ClasspathFile readFile(String path) {
            final File file = new File(directory, File.separatorChar == '/' ? path : path.replace('/', File.separatorChar));
            if (file.exists()) {
                try {
                    return new ClasspathFile(Files.toBytes(file), this);
                } catch (IOException ioException) {
                    ProgramWarning.message("Error reading from " + file + ": " + ioException);
                    return null;
                }
            }
            return null;
        }

        @Override
        public File file() {
            return directory;
        }

        @Override
        public boolean isDirectory() {
            return true;
        }
    }

    /**
     * Represents a classpath entry that is a path to an existing zip/jar archive file.
     */
    static final class Archive extends Entry {

        private final File file;
        private ZipFile zipFile;

        public Archive(File file) {
            this.file = file;
        }

        @Override
        public ZipFile zipFile() {
            if (zipFile == null && file != null) {
                try {
                    zipFile = new ZipFile(file);
                } catch (IOException e) {
                    ProgramWarning.message("Error opening ZIP file: " + file.getPath());
                    e.printStackTrace();
                }
            }
            return zipFile;
        }

        @Override
        ClasspathFile readFile(String path) {
            final ZipFile zf = zipFile();
            if (zf == null) {
                return null;
            }
            try {
                final ZipEntry zipEntry = zf.getEntry(path);
                if (zipEntry != null) {
                    return new ClasspathFile(readZipEntry(zf, zipEntry), this);
                }
            } catch (IOException ioException) {
                //ProgramWarning.message("could not read ZIP file: " + file);
            }
            return null;
        }

        @Override
        public File file() {
            return file;
        }

        @Override
        public boolean isArchive() {
            return true;
        }
    }

    /**
     * Gets the ordered entries from which this classpath is composed.
     *
     * @return a sequence of {@code Entry} objects
     */
    public List<Entry> entries() {
        return entries;
    }

    /**
     * Creates a classpath {@link Entry} from a given file system path.
     *
     * @param path a file system path denoting a classpath entry
     */
    public static Entry createEntry(String path) {
        final File pathFile = new File(path);
        if (pathFile.isDirectory()) {
            return new Directory(pathFile);
        } else if (path.endsWith(".zip") || path.endsWith(".jar")) {
            if (pathFile.exists() && pathFile.isFile()) {
                return new Archive(pathFile);
            }
        }
        //ProgramWarning.message("Class path entry is neither a directory nor a JAR file: " + path);
        return new PlainFile(pathFile);
    }

    /**
     * Creates a new classpath from an array of classpath entries.
     *
     * @param paths an array of classpath entries
     */
    public Classpath(String[] paths) {
        final Entry[] entryArray = new Entry[paths.length];
        for (int i = 0; i < paths.length; ++i) {
            final String path = paths[i];
            entryArray[i] = createEntry(path);
        }
        this.entries = Arrays.asList(entryArray);
    }

    /**
     * Creates a new classpath from a sequence of classpath entries.
     *
     * @param paths a sequence of classpath entries
     */
    public Classpath(List<Entry> entries) {
        this.entries = entries;
    }

    /**
     * Creates a new classpath by parsing a string of classpath entries separated by the system dependent
     * {@linkplain File#pathSeparator path separator}.
     *
     * @param paths a string of classpath entries separated by ':' or ';'
     */
    public Classpath(String paths) {
        this(paths.split(File.pathSeparator));
    }

    /**
     * Gets the classpath derived from the value of the {@code "java.ext.dirs"} system property.
     *
     * @see "http://java.sun.com/javase/6/docs/technotes/guides/extensions/extensions.html"
     */
    private static String extensionClasspath() {
        final String extDirs = System.getProperty("java.ext.dirs");
        if (extDirs != null) {
            final StringBuilder buf = new StringBuilder();
            for (String extDirPath : extDirs.split(File.pathSeparator)) {
                final File extDir = new File(extDirPath);
                if (extDir.isDirectory()) {
                    for (File file : extDir.listFiles()) {
                        if (file.isDirectory() ||
                            (file.isFile() && (file.getName().endsWith(".jar") || file.getName().endsWith(".zip")))) {
                            if (buf.length() != 0) {
                                buf.append(File.pathSeparatorChar);
                            }
                            buf.append(file.getAbsolutePath());
                        }
                    }
                } else {
                    // Ignore non-directory
                }
            }
            if (buf.length() != 0) {
                buf.append(File.pathSeparatorChar);
                return buf.toString();
            }
        }
        return "";
    }

    /**
     * Gets a classpath corresponding to the class search order used by the application class loader.
     */
    public static Classpath fromSystem() {
        final String value = System.getProperty("sun.boot.class.path") + File.pathSeparator + extensionClasspath() + System.getProperty("java.class.path");
        return new Classpath(value.split(File.pathSeparator));
    }

    /**
     * Gets a classpath corresponding to the class search order used by the boot class loader.
     */
    public static Classpath bootClassPath() {
        final String value = System.getProperty("sun.boot.class.path");
        if (value == null) {
            return EMPTY;
        }
        return new Classpath(value.split(File.pathSeparator));
    }

    /**
     * Gets a new classpath obtained by prepending a given classpath to this class classpath.
     *
     * @param classpath the classpath to prepend to this classpath
     * @return the result of prepending {@code classpath} to this classpath
     */
    public Classpath prepend(Classpath classpath) {
        ArrayList<Entry> entries = new ArrayList<Entry>(this.entries.size() + classpath.entries.size());
        entries.addAll(classpath.entries);
        entries.addAll(this.entries);
        return new Classpath(entries);
    }

    /**
     * Gets a new classpath obtained by prepending a given classpath to this class classpath.
     *
     * @param classpath the classpath to prepend to this classpath
     * @return the result of prepending {@code classpath} to this classpath
     */
    public Classpath prepend(String path) {
        ArrayList<Entry> entries = new ArrayList<Entry>(this.entries.size());
        entries.add(createEntry(path));
        entries.addAll(this.entries);
        return new Classpath(entries);
    }

    /**
     * Searches for a class file denoted by a given class name on this classpath and returns its contents in a byte array if
     * found. Any IO exception that occurs when reading is silently ignored.
     *
     * @param className a fully qualified class name (e.g. "java.lang.Class")
     * @return the contents of the file available on the classpath whose name is computed as
     *         {@code className.replace('.', '/')}. If no such file is available on this class path or if
     *         reading the file produces an IO exception, then null is returned.
     */
    public ClasspathFile readClassFile(String className) {
        return readFile(className, ".class");
    }

    /**
     * Searches for a file denoted by a given class name on this classpath and returns its contents in a byte array if
     * found. Any IO exception that occurs when reading is silently ignored.
     *
     * @param className a fully qualified class name (e.g. "java.lang.Class")
     * @param extension a file extension
     * @return the contents of the file available on the classpath whose name is computed as
     *         {@code className.replace('.', '/') + extension}. If no such file is available on this class path or if
     *         reading the file produces an IO exception, then null is returned.
     */
    public ClasspathFile readFile(String className, String extension) {
        final String path = className.replace('.', '/') + extension;
        for (Entry entry : entries()) {
            ClasspathFile classpathFile = entry.readFile(path);
            if (classpathFile != null) {
                recordPackage(className, classpathFile);
                return classpathFile;
            }
        }
        return null;
    }

    private void recordPackage(String className, ClasspathFile classpathFile) {
        final int ix = className.lastIndexOf('.');
        final String packageName = ix < 0 ? "/" : className.substring(0, ix + 1).replace('.', '/');
        if (!classpathFileMap.containsKey(packageName)) {
            classpathFileMap.put(packageName, classpathFile);
        }
    }

    /**
     * Searches for an existing file corresponding to a directory entry in this classpath composed with a given path
     * suffix.
     *
     * @param suffix a file path relative to a directory entry of this classpath
     * @return a file corresponding to the {@linkplain File#File(File, String) composition} of the first directory entry
     *         of this classpath with {@code suffix} that denotes an existing file or null if so such file exists
     */
    public File findFile(String suffix) {
        for (Entry entry : entries()) {
            if (entry instanceof Directory) {
                final File file = new File(((Directory) entry).directory, suffix);
                if (file.exists()) {
                    return file;
                }
            }
        }
        return null;
    }

    public static byte[] readZipEntry(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        final byte[] bytes = new byte[(int) zipEntry.getSize()];
        final InputStream zipStream = new BufferedInputStream(zipFile.getInputStream(zipEntry), bytes.length);
        try {
            int offset = 0;
            while (offset < bytes.length) {
                final int n = zipStream.read(bytes, offset, bytes.length - offset);
                if (n <= 0) {
                    //ProgramWarning.message("truncated ZIP file: " + zipFile);
                }
                offset += n;
            }
        } finally {
            zipStream.close();
        }
        return bytes;
    }

    @Override
    public String toString() {
        if (entries == null || entries.isEmpty()) {
            return "";
        }
        String s = entries.toString().replace(", ", File.pathSeparator);
        return s.substring(1, s.length() - 1);
    }

    public ClasspathFile classpathFileForPackage(String name) {
        return classpathFileMap.get(name);
    }

    /**
     * Converts this object to a String array with one array element for each classpath entry.
     *
     * @return the newly created String array with one element per classpath entry
     */
    public String[] toStringArray() {
        final String[] result = new String[entries().size()];
        int z = 0;
        for (Classpath.Entry e : entries()) {
            result[z] = e.path();
            z++;
        }
        return result;
    }
}
