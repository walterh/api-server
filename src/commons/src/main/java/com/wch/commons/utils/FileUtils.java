package com.wch.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class FileUtils {
    public static String separator = File.separator;

    // "?:" means non-capturing group. Used for performance
    private static Pattern IMAGE_CONTENT_TYPE_PATTERN = Pattern.compile("image/(gif|jpeg|pjpeg|bmp|tiff|png)");
    private static Pattern IMAGE_EXTENSION_PATTERN = Pattern.compile("(?:gif|jpeg|jpg|pjpeg|bmp|tiff|tif|raw|png)");
    private static Pattern VIDEO_EXTENSION_PATTERN = Pattern.compile("(?:mov|avi|3gp|mp4|flv|ogv|webm)");
    private static Pattern FILENAME_WITH_INDEX_PATTERN = Pattern.compile("^(\\w*\\D)(\\d+)$");

    public static final Pattern DIGIT_PATTERN = Pattern.compile("[\\d]+");

    public static String getTempDirectoryPath() {
        return System.getProperty("java.io.tmpdir");
    }

    public static String concat(String path1, String path2) {
        if (!path1.endsWith(separator)) {
            path1 = path1 + separator;
        }
        if (path2.startsWith(separator)) {
            path2 = path2.substring(separator.length());
        }

        return path1 + path2;
    }

    public static void copyFile(final String sourceFilePath, final String destFilePath) throws Exception {
        copyFile(new File(sourceFilePath), new File(destFilePath));
    }
    
    public static void copyFile(File sourceFile, File destFile) throws Exception {
        if (!destFile.exists()) {
            // also creates intermediate directories.
            org.apache.commons.io.FileUtils.touch(destFile);
        }

        copyNioBuffered(sourceFile.getAbsolutePath(), destFile.getAbsolutePath());

        // http://stackoverflow.com/questions/106770/standard-concise-way-to-copy-a-file-in-java
        //org.apache.commons.io.FileUtils.copyFile(sourceFile, destFile);

        // getting lots of java.nio.channels.ClosedByInterruptException, not sure why
        /*
        		FileChannel source = null;
        		FileChannel destination = null;

        		try {
        			source = new FileInputStream(sourceFile).getChannel();
        			destination = new FileOutputStream(destFile).getChannel();
        			destination.transferFrom(source, 0, source.size());
        		} finally {
        			if (source != null) {
        				source.close();
        			}
        			if (destination != null) {
        				destination.close();
        			}
        		}
        */
    }

    // http://squirrel.pl/blog/2012/06/05/io-vs-nio-interruptions-timeouts-and-buffers/
    private static void copyNioBuffered(String in, String out) throws Exception {
        FileChannel fin = new FileInputStream(in).getChannel();
        FileChannel fout = new FileOutputStream(out).getChannel();

        ByteBuffer buff = ByteBuffer.allocate(4096);
        while (fin.read(buff) != -1 || buff.position() > 0) {
            buff.flip();
            fout.write(buff);
            buff.compact();
        }

        fin.close();
        fout.close();
    }

    public static String readTextFile(String uri) throws Exception {

        String text = null;
        ByteArrayOutputStream os = null;
        try {
            os = new ByteArrayOutputStream();
            readFile(uri, os);
            byte[] fileBytes = os.toByteArray();
            text = new String(fileBytes, UTF_8);
        } finally {
            if (os != null) {
                os.close();
            }
        }

        return text;
    }

    public static void readFile(String uri, OutputStream os) throws Exception {
        FileInputStream fis = null;

        try {
            fis = new FileInputStream(uri);
            StreamUtils.writeStreamToStream(fis, os);

        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Exception e) {
                    log.error("readFile:  exception closing FileInputStream = " + LogUtils.getStackTrace(e));
                }
            }
        }
    }

    public static boolean writeTextFile(String text, String uri) throws Exception {
        byte[] fileBytes = text.getBytes(UTF_8);
        ByteArrayInputStream is = new ByteArrayInputStream(fileBytes);

        return writeFile(uri, is);
    }

    public static boolean writeFile(String uri, InputStream is) throws Exception {
        boolean ret = false;
        FileOutputStream fos = null;

        try {
            if (ensureDirectory(uri)) {
                File f = new File(uri);

                if (f.exists()) {
                    // remove the file first
                    f.delete();
                }

                fos = new FileOutputStream(f);
                writeStreamToStream(is, fos);
            }
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
        return ret;
    }

    private static Charset UTF_8 = Charset.forName("UTF-8");
    private final static int DefaultBlockAllocationSize = 81920;

    public static void writeStreamToStream(InputStream srcStream, OutputStream destStream) throws IOException {
        byte[] byteBuff = new byte[DefaultBlockAllocationSize];
        int count = 0;

        while ((count = srcStream.read(byteBuff, 0, byteBuff.length)) > 0) {
            destStream.write(byteBuff, 0, count);
        }

        destStream.flush();
    }

    public static String conformDirectory(String uriPath) {
        String directory = null;

        if (uriPath.endsWith(File.separator)) {
            // assume this is a directory.
            directory = uriPath;
        } else {
            // try to figure out what this uri is.
            int lastSeparator = uriPath.lastIndexOf(File.separator);
            int lastExtension = uriPath.lastIndexOf('.');

            if (lastSeparator == -1) {
                // hm...this means that we basically have a file
                directory = null;
            } else if (lastExtension == -1) {
                // This means that we have something like "/foo/bar". Assume this was
                // intended as a directory.
                directory = uriPath + File.separator;
            } else if (lastExtension < lastSeparator) {
                // We don't anticipate something like "/foo/bar.x/baz". Assume this was
                // intended as a directory
                directory = uriPath + File.separator;
            } else {
                // this is the typical case: /foo/bar/baz.txt. Extract "/foo/bar/"
                directory = uriPath.substring(0, lastSeparator + 1);
            }
        }

        return directory;
    }

    /*
     * This method examines the path and ensures that the directory exists. If it doesn't, it attempts to create it. There is some heuristic logic to determine
     * if the last part of the path is a folder or a file (has extension or doesn't end in "/")
     */
    public static boolean ensureDirectory(String uriPath) {
        String directory = conformDirectory(uriPath);
        boolean successful = false;

        if (!isNullOrEmptyString(directory)) {
            File dir = new File(directory);

            if (!dir.exists()) {
                try {
                    // make all parent directories as well
                    successful = dir.mkdirs();
                } catch (SecurityException e) {
                    successful = false;
                }
            } else {
                successful = true;
            }
        }

        return successful;
    }

    public static void safeDeleteFile(String localUri) {
        if (!isNullOrEmptyString(localUri)) {
            safeDeleteFile(new File(localUri));
        }
    }

    public static void safeDeleteFile(File f) {
        try {
            if (f != null && f.exists()) {
                if (f.isDirectory()) {
                    for (String child : f.list()) {
                        safeDeleteFile(new File(f, child));
                    }
                } else {
                    f.delete();
                }
            }
        } catch (Exception e) {
        }
    }

    public static String embedPath(String path, String newDir) {
        final String dir = FileUtils.getDirectory(path);
        final String fileName = !path.endsWith("/") ? FileUtils.getFileName(path) : "";
        
        return String.format("%s%s/%s", dir, newDir, fileName);
    }
    
    public static String embedTag(String fileName, String tag) {
        String ext = getExtension(fileName);

        if (!isNullOrEmptyString(ext)) {
            ext = tag + "." + ext;
            return replaceExtension(fileName, ext);
        } else {
            return fileName + "." + tag;
        }
    }

    public static String embedTimestamp(String fileName, long ts) {
        String ext = getExtension(fileName);
        String tsPart = String.format("%016x", ts);

        if (!isNullOrEmptyString(ext)) {
            ext = tsPart + "." + ext;
            return replaceExtension(fileName, ext);
        } else {
            return fileName + "." + tsPart;
        }
    }

    public static String extractLastTag(String fileName) {
        String lastTag = null;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0) {
            int secondLastDot = fileName.lastIndexOf('.', lastDot - 1);

            if (secondLastDot > 0) {
                // extract
                lastTag = fileName.substring(secondLastDot + 1, lastDot);
            }
        }

        return lastTag;
    }

    public static String removeTags(String fileName, List<String> tags) {
        if (!Utils.isNullOrEmptyString(fileName) && !Utils.isNullOrEmpty(tags)) {
            for (String tag : tags) {
                tag = String.format(".%s.", tag);
                // we could use a regex, but it would be horribly expensive.  and it would be "\\.%s\\."
                //fileName = fileName.replaceAll(tag, ".");
                int loc = fileName.indexOf(tag);
                if (loc >= 0) {
                    // length -1 so that we restore a "."
                    fileName = fileName.substring(0, loc) + fileName.substring(loc + tag.length() - 1);
                }
            }
        }

        return fileName;
    }

    public static String removeTag(String fileName, String tag) {
        int loc = !Utils.isNullOrEmptyString(fileName) ? fileName.indexOf(tag) : -1;
        if (loc > 0) {
            fileName = fileName.substring(0, loc) + fileName.substring(loc + tag.length());

            // we may need to remove a double dot.
            if (fileName.charAt(loc - 1) == '.') {
                // is the tag at the end of the file?
                if (fileName.length() > loc) {
                    if (fileName.charAt(loc) == '.') {
                        // remove the second dot
                        fileName = fileName.substring(0, loc) + fileName.substring(loc + 1);
                    }
                }
            }
        }

        return fileName;
    }
    
    public static String removeLastTag(String fileName) {
        String tag = extractLastTag(fileName);
        
        if (!Utils.isNullOrEmptyString(tag)) {
            return removeTag(fileName, tag);
        } else {
            return fileName;
        }
    }

    /*
     * Returns the fileName extension, sans the dot separator
     */
    public static String getExtension(String fileName) {
        int lastExtension = lastExtensionIdx(fileName);
        String ext = null;

        if (lastExtension != -1) {
            ext = fileName.substring(lastExtension + 1).toLowerCase();
        }

        return ext;
    }

    public static String getFileName(String uri) {
        if (uri != null && !uri.isEmpty()) {
            int lastSlash = uri.lastIndexOf('/');
            if (lastSlash > 0) {
                return uri.substring(lastSlash + 1);
            } else {
                return uri;
            }
        }
        return uri;
    }

    public static String getUriSansExtension(String uri) {
        int lastExtension = lastExtensionIdx(uri);

        if (lastExtension > 0) {
            return uri.substring(0, lastExtension);
        } else {
            return uri;
        }
    }

    public static String getFileNameSansExtension(String uri) {
        String fileName = getFileName(uri);
        int lastExtension = lastExtensionIdx(fileName);

        if (lastExtension > 0) {
            return fileName.substring(0, lastExtension);
        } else {
            return fileName;
        }
    }

    public static String getDirectory(String uri) {
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash > 0) {
            return uri.substring(0, lastSlash + 1);
        } else {
            return "/";
        }
    }

    public static String replaceExtension(String fileName, String newExtension) {
        int lastExtension = lastExtensionIdx(fileName);

        newExtension = newExtension.toLowerCase();

        // be nice to the input...accept both ".xxx" and "xxx"
        if (newExtension.startsWith(".")) {
            newExtension = newExtension.substring(1);
        }

        if (lastExtension < 0) {
            fileName = fileName + "." + newExtension;
        } else {
            // +1 to skip the dot.
            fileName = fileName.substring(0, lastExtension + 1) + newExtension;
        }

        return fileName;
    }

    public static Integer lastExtensionIdx(String fileName) {
        return (fileName != null) ? fileName.lastIndexOf('.') : -1;
    }

    public static String replaceLast(String fileName, String a, String b) {
        if (!Utils.isNullOrEmptyString(fileName)) {
            int idx = fileName.lastIndexOf(a);
            if (idx >= 0) {
                fileName = fileName.substring(0, idx) + b + fileName.substring(idx + a.length());
            }
        }

        return fileName;
    }

    public static String increment(String fileName) {
        final String fileNameSansExtension = FileUtils.getFileNameSansExtension(fileName);
        final String ext = FileUtils.getExtension(fileName);
        final String possibleDigitExt = FileUtils.getExtension(fileNameSansExtension);

        if (!Utils.isNullOrEmptyString(possibleDigitExt) && DIGIT_PATTERN.matcher(possibleDigitExt).matches()) {
            final String fileNameSansDigits = FileUtils.getFileNameSansExtension(fileNameSansExtension);
            final Long digits = Long.valueOf(possibleDigitExt);

            fileName = String.format("%s.%d.%s", fileNameSansDigits, digits + 1, ext);
        } else {
            final Matcher m = FILENAME_WITH_INDEX_PATTERN.matcher(fileNameSansExtension);

            if (m.matches()) {
                final String fileNameSansDigits = m.group(1);
                final Long digits = Long.valueOf(m.group(2));

                fileName = String.format("%s%d.%s", fileNameSansDigits, digits + 1, ext);
            } else {
                fileName = String.format("%s1.%s", fileNameSansExtension, ext);
            }
        }

        return fileName;
    }

    public static Boolean isNullOrEmptyString(String s) {
        return s == null || s.length() == 0;
    }

    public static boolean isValidImageFileExtension(String ext) {
        return IMAGE_EXTENSION_PATTERN.matcher(ext).matches();
    }

    public static boolean isValidVideoFileExtension(String ext) {
        return VIDEO_EXTENSION_PATTERN.matcher(ext).matches();
    }

    public static String getExtensionFromContentType(String contentType) {
        String ext = null;
        Matcher m = IMAGE_CONTENT_TYPE_PATTERN.matcher(contentType);
        if (m.matches()) {
            ext = m.group(1);

            // fixup
            if (ext.compareTo("jpeg") == 0) {
                ext = "jpg";
            }
        } else {
            ext = contentTypeToExtension(contentType);
        }

        return ext;
    }

    public static String inferContentTypeFromFileName(String fileName) {
        String ext = getExtension(fileName);
        String contentType = null;

        if (IMAGE_EXTENSION_PATTERN.matcher(ext).matches()) {
            if (ext.equals("jpg")) {
                ext = "jpeg";
            }
            contentType = "image/" + ext;
        } else if (VIDEO_EXTENSION_PATTERN.matcher(ext).matches()) {
            contentType = fileExtensionToContentType(ext);
        } else if (ext.equals("json")) {
            contentType = "application/json;charset=UTF-8";
        }

        return contentType;
    }

    public static String contentTypeToExtension(String ct) {
        String ext = null;

        if (ct.equals("video/3gpp")) {
            ext = "3gp";
        } else if (ct.equals("video/quicktime")) {
            ext = "mov";
        } else if (ct.equals("video/mp4")) {
            ext = "mp4";
        } else if (ct.equals("video/webm")) {
            ext = "webm";
        } else if (ct.equals("video/x-flv")) {
            ext = "flv";
        } else if (ct.equals("application/ogg")) {
            ext = "ogv";
        }

        return ext;
    }

    public static String fileExtensionToContentType(String ext) {
        String ct = null;

        ext = ext.toLowerCase();

        if (ext.equals("3gp")) {
            ct = "video/3gpp";
        } else if (ext.equals("mov")) {
            ct = "video/quicktime";
        } else if (ext.equals("mp4")) {
            ct = "video/mp4";
        } else if (ext.equals("webm")) {
            ct = "video/webm";
        } else if (ext.equals("flv")) {
            ct = "video/x-flv";
        } else if (ext.equals("ogv")) {
            ct = "application/ogg";
        }

        return ct;
    }

    public static String probeFileCharacteristics(final String localFilePath, final String directory, final String tag) {
        // wtf is going on?
        final StringBuilder sbb = new StringBuilder();

        try {
            if (!Utils.isNullOrEmptyString(localFilePath)) {
                final File localSourceFile = new File(localFilePath);
                if (!localSourceFile.exists()) {
                    final String log = String.format("\t\tfile does not exist:  %s\n", localFilePath);
                    sbb.append(log);
                } else if (localSourceFile.length() == 0) {
                    final String log = String.format("\t\tfile has zero length:  %s\n", localFilePath);
                    sbb.append(log);
                } else {
                    String log = String.format("\t\tfile has size:  %d\n", localSourceFile.length());
                    sbb.append(log);

                    // see if we can touch it (readable)
                    try {
                        org.apache.commons.io.FileUtils.touch(localSourceFile);
                        log = String.format("\t\tfile can be touched:  %s\n", localFilePath);
                        sbb.append(log);
                    } catch (Exception ee) {
                        log = String.format("\t\tfile cannot be touched:  %s\n", localFilePath);
                        sbb.append(log);
                    }
                    try {
                        FileInputStream fis = new FileInputStream(localSourceFile);
                        log = String.format("\t\tfile can be opened:  %s\n", localFilePath);
                        sbb.append(log);

                        fis.close();
                    } catch (Exception ee) {
                        log = String.format("\t\tfile cannot be opened:  %s\n", localFilePath);
                        sbb.append(log);
                    }
                }
            }

            // get a directory dump
            final File[] tempFiles = !Utils.isNullOrEmptyString(tag) ? new File(directory).listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.contains(tag);
                }
            }) : new File(directory).listFiles();

            // sort by date
            if (tempFiles != null && tempFiles.length > 0) {
                Arrays.sort(tempFiles, new Comparator<File>() {
                    public int compare(File arg0, File arg1) {
                        return Long.valueOf(arg0.lastModified()).compareTo(Long.valueOf(arg1.lastModified()));
                    }
                });

                sbb.append(String.format("\t%d files in %s:\n", tempFiles.length, directory));

                for (File tempFile : tempFiles) {
                    if (tempFile.isFile()) {
                        sbb.append(String.format("\t\t(%12d) %s\n", tempFile.length(), tempFile.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception e) {
            sbb.append("probeFileCharacteristics failed due to " + LogUtils.getStackTrace(e));
        }
        return sbb.toString();
    }
}