package com.mgh14.codegraph;

import java.io.Serializable;
import java.util.ArrayList;

/** TODO: Document */
public class ForLookingAtBytesClass implements Serializable {

  @SuppressWarnings("unused")
  private static final String STATIC_STRING = "Static string";
  @SuppressWarnings("unused")
  private static final Object obj;

  static {
    obj = new ArrayList<>();
  }

  @SuppressWarnings({"unused", "WeakerAccess"})
  public ForLookingAtBytesClass(int x) {
    final String concat = "y";
    final String concat2 = "y" + getAString();
    int t = 5;
  }

  @SuppressWarnings("unused")
  public ForLookingAtBytesClass() {}

  @SuppressWarnings("WeakerAccess")
  String getAString() {
    return "AString";
  }

  @SuppressWarnings("unused")
  public static ForLookingAtBytesClass createObject() {
    final ForLookingAtBytesClass forLookingAtBytesClass = new ForLookingAtBytesClass(10230);
    final String aString = forLookingAtBytesClass.getAString();

    return forLookingAtBytesClass;
  }

  @SuppressWarnings("unused")
  public String getSecondString() {
    return "secondString";
  }
}
