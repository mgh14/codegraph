package com.mgh14.codegraph;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

/** */
@Value
@Builder
@EqualsAndHashCode
public class MethodReference {
  String parentClass;
  int access;
  boolean isConstructor;
  boolean isStatic;
  String name;
  String desc;
  String signature;
  String[] exceptions;

  @Override
  public String toString() {
    return String.format(
        "parent:[%s]::access:[%s];name:[%s];desc:[%s][%s];signature:[%s];exceptions:[%s];s-o-i:[%s]",
        parentClass,
        access,
        name,
        isConstructor ? "CONSTRUCTOR" : StringUtils.EMPTY,
        desc,
        signature,
        Arrays.toString(exceptions),
        isStatic ? "stat" : "inst");
  }
}
