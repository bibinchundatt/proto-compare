package com.tool.test.compare;

import com.squareup.protoparser.DataType;
import com.squareup.protoparser.FieldElement;
import com.squareup.protoparser.MessageElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtoCallerTree {

  private DirectoryData directoryData;
  private Map<String, ProtoElementNode> elementNodeMap;

  public ProtoCallerTree(DirectoryData directoryData) {
    this.directoryData = directoryData;
    elementNodeMap = new HashMap<>();
  }

  class ProtoElementNode {

    String protoName;
    private List<String> parents = null;
    private List<String> childs = null;

    public ProtoElementNode(String protoName) {
      this.protoName = protoName;
    }

    public List<String> getParents() {
      return parents;
    }

    public List<String> getChilds() {
      return childs;
    }

    public void addToParent(String element) {
      if (parents == null) {
        parents = new ArrayList<>(1);
      }
      parents.add(element);
    }

    public void addToChild(String element) {
      if (childs == null) {
        childs = new ArrayList<>(1);
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
  }

  private void updateNode(ProtoElementNode node, FieldElement fieldElement) {
    DataType type = fieldElement.type();
    String name = type.toString();
    ProtoElementNode currentNode = elementNodeMap
        .putIfAbsent(name, new ProtoElementNode(name));
    if (currentNode == null) {
      currentNode = elementNodeMap.get(name);
    }
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