package com.mgh14.codegraph;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/** */
class CodeGraphAppTest {

  private static final Class<?> TEST_CLASS = ForLookingAtBytesClass.class;

  @Test
  void getAllCallGraphs_successResult() throws ClassNotFoundException {
    Map<MethodReference, CodeGraphApp.CallTreeNode> result =
        CodeGraphApp.getAllCallGraphs(TEST_CLASS);

    assertThat(result.entrySet(), hasSize(7));
    // TODO: these aren't great test assertions, but MVP takes priority right now:
    Set<String> methodOwners =
        result.keySet().stream().map(MethodReference::getParentClass).collect(Collectors.toSet());
    assertThat(
        methodOwners,
        org.hamcrest.Matchers.containsInAnyOrder(
            "com/mgh14/codegraph/ForLookingAtBytesClass", "java/lang/Object"));
    Set<String> valueMethodOwners =
        result.values().stream()
            .map(CodeGraphApp.CallTreeNode::getOwner)
            .collect(Collectors.toSet());
    assertThat(valueMethodOwners, hasSize(1));
    assertThat(valueMethodOwners, hasItem("com/mgh14/codegraph/ForLookingAtBytesClass"));
    Set<CodeGraphApp.CallTreeNode> nodesWithAnyChildren =
        result.values().stream()
            .filter(node -> node.getChildren().size() > 0)
            .collect(Collectors.toSet());
    Set<CodeGraphApp.CallTreeNode> nodesWithTwoOrMoreChildren =
        result.values().stream()
            .filter(node -> node.getChildren().size() > 1)
            .collect(Collectors.toSet());
    Set<CodeGraphApp.CallTreeNode> nodesWithThreeOrMoreChildren =
        result.values().stream()
            .filter(node -> node.getChildren().size() > 2)
            .collect(Collectors.toSet());
    assertThat(nodesWithAnyChildren, hasSize(3));
    assertThat(nodesWithTwoOrMoreChildren, hasSize(2));
    assertThat(nodesWithThreeOrMoreChildren, hasSize(0));
  }
}