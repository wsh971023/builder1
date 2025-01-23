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
        if (dirPath == null || !Files.exists(Path.of(dirPath))) {
            return;
        }

        Files.walkFileTree(Path.of(dirPath), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                // 修复点1: 清除只读属性（Windows 必需）
                if (file.toFile().exists()) {
                    file.toFile().setWritable(true);
                }
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc; // 确保异常传播
                }
                // 修复点2: 清除目录只读属性
                if (dir.toFile().exists()) {
                    dir.toFile().setWritable(true);
                }
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
