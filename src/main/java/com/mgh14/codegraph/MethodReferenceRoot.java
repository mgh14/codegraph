package com.mgh14.codegraph;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/** */
@Value
@Builder
public class MethodReferenceRoot {
  MethodReference parentReference;
  List<MethodReference> childReferences;
}
