package com.camunda.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.decisionService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTask;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.findId;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecisionTableResult;
import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.extension.process_test_coverage.junit5.ProcessEngineCoverageExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(ProcessEngineCoverageExtension.class)
public class ProcessJUnitTest {
  
  @Test
  @Deployment(resources = {"tweetApproval.dmn", "process.bpmn"})
  public void testHappyPath() {
    Map<String, Object> variables = new HashMap<>();
    variables.put("email", "info@javawhizz.com");
    variables.put("content", "this should be published");
    ProcessInstance processInstance =
        runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

    List<Job> jobList = jobQuery().processInstanceId(processInstance.getId())
        .list();

    assertThat(jobList).hasSize(1);

    Job job = jobList.get(0);

    execute(job);

    assertThat(processInstance).isEnded();

  }

  @Test
  @Deployment(resources = "process.bpmn")
  public void testTweetRejected(){
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("content", "Hello David");
    varMap.put("approved", false);

    ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("TwitterQAProcess")
        .setVariables(varMap)
        .startAfterActivity(findId("Review Tweet"))
        .execute();

//    assertThat(processINstance).isEnded().hasPassed(findId("Tweet rejected"));

    assertThat(processInstance)
        .isWaitingAt(findId("Notify user of rejection"))
        .externalTask()
        .hasTopicName("notification");

    complete(externalTask());
  }

  @Test
  @Deployment(resources = "process.bpmn")
  public void testSuperUserTweet(){
      ProcessInstance processInstance = runtimeService().createMessageCorrelation("superuserTweet")
          .setVariable("content", "My Exercise 11 Tweet David- " + System.currentTimeMillis())
          .correlateWithResult()
          .getProcessInstance();

      assertThat(processInstance).isStarted();

    List<Job> jobList = jobQuery().processInstanceId(processInstance.getId())
        .list();

    assertThat(jobList).hasSize(1);

    Job job = jobList.get(0);

    execute(job);

    assertThat(processInstance).isEnded();
  }

//  @Test
  @Deployment(resources = "process.bpmn")
  public void testTweetWithdrawn(){
    Map<String, Object> varMap = new HashMap<>();
    varMap.put("content", "Test tweetWithdrawn message");

    ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", varMap);

    assertThat(processInstance).isStarted().isWaitingAt(findId("Review Tweet"));

    runtimeService()
        .createMessageCorrelation("tweetWithdrawn")
        .processInstanceVariableEquals("content", "Test tweetWithdrawn message")
        .correlateWithResult();

    assertThat(processInstance).isEnded();
  }

  @Test
  @Deployment(resources = {"tweetApproval.dmn", "process.bpmn"})
  public void testTweetFromJacob(){
    Map<String, Object> variables = withVariables("email", "info@javawhizz.com", "content", "this should be published");
    DmnDecisionTableResult decisionResult = decisionService()
        .evaluateDecisionTableByKey("tweetApproval", variables);
    assertThat(decisionResult.getFirstResult()).contains(entry("approved", true));
  }

}
