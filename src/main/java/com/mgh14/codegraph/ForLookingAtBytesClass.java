package com.mgh14.codegraph;

import java.io.Serializable;
import java.util.ArrayList;

/** TODO: Document */
public class ForLookingAtBytesClass implements Serializable {

  private static final String STATIC_STRING = "Static string";
  private static final Object obj;

  static {
    obj = new ArrayList<>();
  }

  public ForLookingAtBytesClass(int x) {
    final String concat = "y";
    final String concat2 = "y" + getAString();
    int t = 5;
  }

  public ForLookingAtBytesClass() {}

  String getAString() {
    return "AString";
  }

  public static ForLookingAtBytesClass createObject() {
    final ForLookingAtBytesClass flabcClass = new ForLookingAtBytesClass(10230);
    final String aString = flabcClass.getAString();

    return flabcClass;
  }

  public String getSecondString() {
    return "secondString";
  }
}
