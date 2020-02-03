package com.tool.test.compare;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold proto level incompatibility. Final DC which will hold everything
 */
public class IncompatibilityMetaData {
  private List<String> missingInVersion2;
  private List<String> missingInVersion1;

  private List<String> incompatibles;

  public IncompatibilityMetaData() {
    missingInVersion2 = new ArrayList<>();
    missingInVersion1 = new ArrayList<>();
    incompatibles=new ArrayList<>();
  }

  public void addMissingVersion1(String protoName) {
    missingInVersion1.add(protoName);
  }

  public void addMissingVersion2(String protoName) {
    missingInVersion2.add(protoName);
  }

  public List<String> getMissingInVersion1() {
    return missingInVersion1;
  }

  public List<String> getIncompatibles() {
    return incompatibles;
  }

  public void addIncompatibles(String incompatible) {
    incompatibles.add(incompatible);
  }

  public List<String> getMissingInVersion2() {
    return missingInVersion2;
  }
}
