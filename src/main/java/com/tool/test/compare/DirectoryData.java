package com.tool.test.compare;

import com.google.common.collect.ImmutableMap;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.TypeElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Directory meta data
 */
public class DirectoryData {
  private static final Logger LOG =
      LoggerFactory.getLogger(DirectoryData.class);

  private Map<String, File> fileMap;
  private Map<String, TypeElement> protoTypeToElement;
  private File baseDir;

  public DirectoryData(File baseDir) {
    this.baseDir = baseDir;
    fileMap = new HashMap<>();
    protoTypeToElement = new HashMap<>();
  }

  public void addFile(Path path) {
    File file = path.toFile();
    fileMap.put(file.getName(), file);
  }

  public File getFile(String name) {
    return fileMap.get(name);
  }

  public Map<String, TypeElement> getProtoTypeToElement() {
    return ImmutableMap.copyOf(protoTypeToElement);
  }

  public void addProtoFile(File file, ProtoFile path) {
    path.typeElements().iterator().forEachRemaining(typeElement -> {
      if (protoTypeToElement.put(typeElement.name(), typeElement) != null) {
        LOG.error("DuplicateEntry added to ProtoName");
      }
    });
  }

  public HashMap<String, File> getProtoFilesMap() {
    return new HashMap<>(fileMap);
  }

  public File getBaseDir() {
    return baseDir;
  }

  public void setBaseDir(File baseDir) {
    this.baseDir = baseDir;
  }
}