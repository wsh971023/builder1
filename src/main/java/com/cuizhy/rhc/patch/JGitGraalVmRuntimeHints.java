package com.cuizhy.rhc.patch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * 临时修正 jgit 的反射和序列化问题 以适配GraalVm JDK 二进制构建文件
 * @see <a href="https://github.com/eclipse-jgit/jgit/issues/103">issue 地址</a>
 */
@Configuration
@ImportRuntimeHints(JGitGraalVmRuntimeHints.class)
@Slf4j
public class JGitGraalVmRuntimeHints implements RuntimeHintsRegistrar {

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator)).forEach(classpathEntry -> {
            // If the classpathEntry is no jar skip it
            if (!classpathEntry.endsWith(".jar")) {
                return;
            }

            try (JarInputStream is = new JarInputStream(Files.newInputStream(Path.of(classpathEntry)))) {
                JarEntry entry = is.getNextJarEntry();
                while (entry != null) {
                    String entryName = entry.getName();
                    if (entryName.endsWith(".class") && entryName.startsWith("org/eclipse/jgit") && !entryName.contains("package-info")) {
                        String githubApiClassName = entryName.replace("/", ".");
                        String githubApiClassNameWithoutClass = githubApiClassName.substring(0, githubApiClassName.length() - 6);
                        log.info("Registered class {} for reflections and serialization.", githubApiClassNameWithoutClass);
                        hints.reflection().registerType(TypeReference.of(githubApiClassNameWithoutClass), MemberCategory.values());
                        hints.serialization().registerType(TypeReference.of(githubApiClassNameWithoutClass));
                    }
                    entry = is.getNextJarEntry();
                }
            } catch (IOException e) {
                log.warn("Error while reading jars", e);
            }
        });

        hints.reflection()
            .registerType(TypeReference.of(IOException.class),
                hint -> hint.withMembers(MemberCategory.values())
            );

        hints.resources()
            .registerPattern("application.yml");
    }
}