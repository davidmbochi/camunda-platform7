package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreateTweetDelegate implements JavaDelegate {
    private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class);

    TwitterService twitter = new TwitterService();

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String content = (String) execution.getVariable("content");
        if (content.equals("Network error"))
            throw new RuntimeException("simulated network error");
        twitter.updateStatus(content);

    }
}
