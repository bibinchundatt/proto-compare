package com.tool.test.compare;

import com.squareup.protoparser.*;
import com.sun.deploy.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class ProtoCompatabilityInsight {

  private static final Logger LOG =
      LoggerFactory.getLogger(ProtoCompatabilityInsight.class);

  private static IncompatibilityMetaData incompatabilityMetaData;

  public static void main(String[] args) throws IOException {

    File baseDir = new File("proto-compare");
    if (!baseDir.exists()) {
      LOG.error("!!!!!!!!!!Directory <proto-compare> is empty!!!!!!!");
    }

    args = new String[] { "272", "312" };

    incompatabilityMetaData = new IncompatibilityMetaData();

    if (args.length != 2) {
      LOG.error("!!!!!!!! Directories to compare not mentioned!!!!");
      LOG.error("Command format");
      LOG.error("{} <old version> <new Version>",
          ProtoCompatabilityInsight.class.getCanonicalName());
      LOG.error("!!!!!!!!------------------------------------!!!!!");
    }

    LOG.info("!!!!!!!!!!! Comparing Proto directories !!!!!!!!!!!!!!!");
    LOG.info("Old version --> Version 1 {}", args[0]);
    LOG.info("New version --> Version 2 {}", args[1]);
    LOG.info("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    DirectoryData version1 = scanProtoDirectory(new File(baseDir, args[0]));
    DirectoryData version2 = scanProtoDirectory(new File(baseDir, args[1]));
    compareProtoDirectories(version1, version2);

    // Parse proto file
    Map<String, File> version1Map = version1.getProtoFilesMap();
    Map<String, File> version2Map = version2.getProtoFilesMap();
    version1Map.entrySet().iterator()
        .forEachRemaining(addFileToDirConsumer(version1));
    version2Map.entrySet().iterator()
        .forEachRemaining(addFileToDirConsumer(version2));

    // Create Tree
    ProtoCallerTree callerTree = new ProtoCallerTree(version1);
    callerTree.parseDirectoryData();

    compareProtoElements(version1, version2);
    compareServiceElements(version1,version2);

    LOG.info("##############Service names #########");
    printAllServiceNames(version1);

    LOG.info("##############Trace print #########");
    printTrace(callerTree);
  }

  private static void printAllServiceNames(DirectoryData version1) {
    Map<String, Map<String, RpcElement>> serviceRpcMapV1 =
        version1.getServiceRpcMap();
    serviceRpcMapV1.keySet().iterator()
        .forEachRemaining(s -> System.out.println(s));
  }

  private static void compareServiceElements(DirectoryData version1,
      DirectoryData version2) {
    Map<String, Map<String, RpcElement>> serviceRpcMapV1 =
        version1.getServiceRpcMap();
    Map<String, Map<String, RpcElement>> serviceRpcMapV2 =
        version2.getServiceRpcMap();

    serviceRpcMapV1.entrySet().iterator().forEachRemaining(mapEntry -> {
      String serviceNameV1 = mapEntry.getKey();
      Map<String, RpcElement> stringRpcElementMapv1 = mapEntry.getValue();
      if (!serviceRpcMapV2.containsKey(serviceNameV1)) {
        LOG.info("Missing Service = " + serviceNameV1);
      } else {
        Map<String, RpcElement> stringRpcElementMapv2 =
            serviceRpcMapV2.get(serviceNameV1);
        stringRpcElementMapv1.forEach((rpcname, elem) -> {
          if (!stringRpcElementMapv2.get(rpcname).equals(elem)) {
            LOG.info("Missing api in version 2 = " + elem);
          }
        });
      }
    });
  }

  private static void printTrace(ProtoCallerTree callerTree) {
    Map<String, ProtoCallerTree.ProtoElementNode> elementNodeMap =
        callerTree.getElementNodeMap();

    for (String missingInVersion1 : incompatabilityMetaData
        .getIncompatibles()) {
      ProtoCallerTree.ProtoElementNode protoElementNode =
          elementNodeMap.get(missingInVersion1);
      System.out.println("///////////Proto/////////////////   " + protoElementNode
          .getProtoName());
      printParentTrace(callerTree, protoElementNode, elementNodeMap, 0);
      System.out.println();
    }
  }

  private static void printParentTrace(ProtoCallerTree callerTree,
      ProtoCallerTree.ProtoElementNode protoElementNode,
      Map<String, ProtoCallerTree.ProtoElementNode> elementNodeMap, int depth) {
    if (protoElementNode.getParents() != null) {
      System.out.println();
      for (int i = 0; i < depth; i++) {
        System.out.print("    ");
      }
      for (String parent : protoElementNode.getParents()) {
        ProtoCallerTree.ProtoElementNode tmp = elementNodeMap.get(parent);
        if (tmp != null && !tmp.protoName.equals(protoElementNode.protoName)) {
          System.out.print(tmp.getProtoName());
          printParentTrace(callerTree, elementNodeMap.get(parent),
              elementNodeMap, depth + 1);
          System.out.println();
          for (int i = 0; i < depth; i++) {
            System.out.print("    ");
          }
        }
      }
    } else {
      Map<String, String> protoServiceName = callerTree.getProtoServiceName();
      String serviceName =
          protoServiceName.get(protoElementNode.getProtoName());
      if (null != serviceName) {
        System.out.println();
        for (int i = 0; i <= depth; i++) {
          System.out.print("    ");
        }
        System.out.print(serviceName);
      }
    }
  }

  private static Consumer<Map.Entry<String, File>> addFileToDirConsumer(
      DirectoryData directoryData) {
    return stringFileEntry -> {
      try {
        directoryData.addProtoFile(stringFileEntry.getValue(),
            ProtoParser.parseUtf8(stringFileEntry.getValue()));
      } catch (IOException e) {
        LOG.error("Exception in converting proto", e);
      }
    };
  }

  private static void compareProtoElements(DirectoryData version1,
      DirectoryData version2) {

    // Compare both version
    Map<String, TypeElement> version1protoTypeToElement =
        version1.getProtoTypeToElement();
    Map<String, TypeElement> version2protoTypeToElement =
        version2.getProtoTypeToElement();

    Set<String> version1ProtoNames =
        new HashSet<>(version1protoTypeToElement.keySet());

    Set<String> version2ProtoNames =
        new HashSet<>(version2protoTypeToElement.keySet());
    LOG.info("Version Proto Message Count : [version 1={} , version 2={}]",
        version1ProtoNames.size(), version2ProtoNames.size());

    for (String version1ProtoName : version1ProtoNames) {
      //compare TypeElement
      if (!version2ProtoNames.contains(version1ProtoName)) {
        incompatabilityMetaData.addMissingVersion1(version1ProtoName);
      } else {
        TypeElement version1TypeElement =
            version1protoTypeToElement.get(version1ProtoName);
        TypeElement version2TypeElement =
            version2protoTypeToElement.get(version1ProtoName);

        if (!version1TypeElement.equals(version2TypeElement)) {
          if (!isForwardCompatible(version1TypeElement, version2TypeElement)) {
            LOG.error("Mismatch between protos for {} ", version1ProtoName);
            LOG.error("Version 1 {} ", version1.getBaseDir());
            prettyPrintElement(version1TypeElement);
            LOG.error("Version 2 {} ", version2.getBaseDir());
            prettyPrintElement(version2TypeElement);
            incompatabilityMetaData.addIncompatibles(version1ProtoName);
          }
        }
      }
    }

    for (String version2ProtoName : version2ProtoNames) {
      if (!version1ProtoNames.contains(version2ProtoName)) {
        incompatabilityMetaData.addMissingVersion2(version2ProtoName);
      }
    }

    LOG.info("Missing Proto Message in version 2 {}",
        StringUtils.join(incompatabilityMetaData.getMissingInVersion2(), ","));
    LOG.info("Missing Proto Message in version 1 {}",
        StringUtils.join(incompatabilityMetaData.getMissingInVersion1(), ","));
  }

  private static void prettyPrintElement(TypeElement typeElement) {
    if (typeElement instanceof MessageElement) {
      List<FieldElement> fields = ((MessageElement) typeElement).fields();
      for (FieldElement felement : fields) {
        LOG.info("         " + toString(felement));
      }
    }
    else if (typeElement instanceof  EnumElement) {
      EnumElement enumElement = (EnumElement) typeElement;
      List<EnumConstantElement> fields = enumElement.constants();
      for ( EnumConstantElement field: fields) {
        LOG.info("         "  + field);
      }
    }
  }

  public static String toString(FieldElement element) {
    return "FieldElement{label=" + element.label() + ", " + "type=" + element
        .type() + ", " + "name=" + element.name() + ", " + "tag=" + element
        .tag() + ", " + "options=" + element.options() + "}";
  }

  private static boolean isForwardCompatible(TypeElement version1TypeElement,
      TypeElement version2TypeElement) {
    boolean isForWardCompatible = true;
    LOG.error("");
    if (version1TypeElement instanceof MessageElement) {
      MessageElement messageElementv1 = (MessageElement) version1TypeElement;
      MessageElement messageElementv2 = (MessageElement) version2TypeElement;
      {
        for (FieldElement ev1 : messageElementv1.fields()) {
          if (!messageElementv2.fields().contains(ev1)) {

            LOG.error("Incompatible Element {}", toString(ev1));
            isForWardCompatible = false;
          }
        }
      }
    } else if (version1TypeElement instanceof EnumElement){
      EnumElement enumElementV1 = (EnumElement) version1TypeElement;
      EnumElement enumElementV2 = (EnumElement) version2TypeElement;
      List<EnumConstantElement> fields = enumElementV1.constants();
      for ( EnumConstantElement fieldV1: fields) {
        if(!enumElementV2.constants().contains(fieldV1)){
          LOG.error("Incompatible Enum {}", fieldV1);
          isForWardCompatible = false;
        }
      }
    }
    return isForWardCompatible;
  }

  private static void compareProtoDirectories(DirectoryData version1,
      DirectoryData version2) {

    // Print missing files
    Map<String, File> version1Map = version1.getProtoFilesMap();
    Map<String, File> version2Map = version2.getProtoFilesMap();

    Set<String> version1FileNames = new HashSet<>(version1Map.keySet());
    Set<String> version2FileNames = new HashSet<>(version2Map.keySet());
    LOG.info("Version filecount : [version 1={} , version 2={}]",
        version1FileNames.size(), version2FileNames.size());

    for (String version1FileName : version1FileNames) {
      if (!version2FileNames.contains(version1FileName)) {
        LOG.info("Missing Proto file in version 2 {}", version1FileName);
      }
    }

    for (String version2FileName : version2FileNames) {
      if (!version1FileNames.contains(version2FileName)) {
        LOG.info("Missing Proto file in version 1 {}", version2FileName);
      }
    }

  }

  public static DirectoryData scanProtoDirectory(File directory)
      throws IOException {
    DirectoryData directoryData = new DirectoryData(directory);
    Files.walk(Paths.get(directory.getPath())).filter(Files::isRegularFile)
        .forEach(file -> directoryData.addFile(file));
    return directoryData;
  }

}
