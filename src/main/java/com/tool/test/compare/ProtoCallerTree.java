package com.tool.test.compare;

import com.squareup.protoparser.DataType;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;
import com.squareup.protoparser.RpcElement;
import com.squareup.protoparser.ServiceElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProtoCallerTree {

  private DirectoryData directoryData;
  private Map<String, ProtoElementNode> elementNodeMap;
  private Map<String, String> protoServiceName;

  public ProtoCallerTree(DirectoryData directoryData) {
    this.directoryData = directoryData;
    elementNodeMap = new HashMap<>();
    protoServiceName = new HashMap<>();
  }

  public DirectoryData getDirectoryData() {
    return directoryData;
  }

  public Map<String, String> getProtoServiceName() {
    return protoServiceName;
  }

  class ProtoElementNode {

    String protoName;
    private Set<String> parents = null;
    private Set<String> childs = null;

    public ProtoElementNode(String protoName) {
      this.protoName = protoName;
    }

    public Set<String> getParents() {
      return parents;
    }

    public Set<String> getChilds() {
      return childs;
    }

    public void addToParent(String element) {
      if (parents == null) {
        parents = new HashSet<>(1);
      }
      parents.add(element);
    }

    public void addToChild(String element) {
      if (childs == null) {
        childs = new HashSet<>(1);
      }
      childs.add(element);
    }

    public String getProtoName() {
      return protoName;
    }
  }

  public Map<String, ProtoElementNode> getElementNodeMap() {
    return elementNodeMap;
  }

  void parseDirectoryData() {
    directoryData.getProtoTypeToElement().forEach((protoName, typeElement) -> {
      elementNodeMap.putIfAbsent(protoName, new ProtoElementNode(protoName));
      ProtoElementNode node = elementNodeMap.get(protoName);
      if (typeElement instanceof MessageElement) {
        MessageElement messageElement = (MessageElement) typeElement;
        messageElement.fields().stream().filter(this::needToUpdateNode)
            .forEach(element -> updateNode(node, element));
      }
    });

    directoryData.getServiceRpcMap()
        .forEach((serviceName, methodNameRPCElem) -> {
          methodNameRPCElem.forEach((methodName, rpcElement) -> {
            protoServiceName.putIfAbsent(rpcElement.requestType().name(),
                serviceName + "#" + methodName);
            protoServiceName.putIfAbsent(rpcElement.responseType().name(),
                serviceName + "#" + methodName);
          });
        });
  }

  private void updateNode(ProtoElementNode node, FieldElement fieldElement) {
    DataType type = fieldElement.type();
    String name = type.toString();
    elementNodeMap.putIfAbsent(name, new ProtoElementNode(name));
    ProtoElementNode currentNode = elementNodeMap.get(name);
    currentNode.addToParent(node.getProtoName());
    node.addToChild(name);
  }

  boolean needToUpdateNode(FieldElement element) {
    if (element.type() instanceof DataType.ScalarType) {
      return false;
    }
    return true;
  }
}
