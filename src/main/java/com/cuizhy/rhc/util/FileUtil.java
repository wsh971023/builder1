package com.cuizhy.rhc.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileUtil {

    /**
     * 获取当前工程的运行路径
     * @return 当前工程的运行路径
     */
    public static File getRuntimePath(){
        URL jarUrl = FileUtil.class.getProtectionDomain().getCodeSource().getLocation();
        String jarPath = jarUrl.getPath();
        return new File(jarPath);
    }

    /**
     * 获取当前工程的运行路径的绝对路径
     * @return 当前工程的运行路径的绝对路径
     */
    public static String getRuntimeAbsolutePath(){
        return getRuntimePath().getParentFile().getAbsolutePath();
    }

    /**
     * 删除文件夹
     * @param dirPath 文件夹路径
     */
    public static void deleteDir(String dirPath) throws IOException {
        if (dirPath == null){
            return;
        }
        if (!Files.exists(Path.of(dirPath))){
            return;
        }
        //使用NIO 因为GraalVm反射问题，尽量不使用三方工具包
        Files.walkFileTree(Path.of(dirPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * 解压文件
     * @param zipFilePath
     * @param destDir
     * @throws IOException
     */
    public static void unzipFile(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(Paths.get(zipFilePath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path filePath = Paths.get(destDir, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    Files.copy(zis, filePath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

}
