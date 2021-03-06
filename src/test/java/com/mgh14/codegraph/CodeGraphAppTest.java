package com.mgh14.codegraph;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

class CodeGraphAppTest {

  // TODO: these tests don't have great assertions, but MVP takes priority right now

  private static final Class<?> TEST_CLASS = ForLookingAtBytesClass.class;

  @Test
  void getAllCallGraphs_successResult() throws ClassNotFoundException {
    Map<String, CodeGraphApp.CallTreeNode> result =
        CodeGraphApp.getAllCallGraphsOneChildDeep(TEST_CLASS);

    assertThat(result.entrySet(), hasSize(7));
    Set<String> methodOwners =
        result.values().stream()
            .map(node -> node.getThisMethodReference().getParentClass())
            .collect(Collectors.toSet());
    String forLookingAtBytesClass = "com/mgh14/codegraph/ForLookingAtBytesClass";
    assertThat(
        methodOwners,
        org.hamcrest.Matchers.containsInAnyOrder(forLookingAtBytesClass, "java/lang/Object"));
    Set<String> valueMethodOwners =
        result.values().stream()
            .map(CodeGraphApp.CallTreeNode::getOwner)
            .collect(Collectors.toSet());
    assertThat(valueMethodOwners, hasSize(1));
    assertThat(valueMethodOwners, hasItem(forLookingAtBytesClass));
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
