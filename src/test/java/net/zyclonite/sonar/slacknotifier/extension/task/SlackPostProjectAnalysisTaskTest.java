package net.zyclonite.sonar.slacknotifier.extension.task;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import net.zyclonite.sonar.slacknotifier.common.SlackNotifierProp;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sonar.api.config.internal.ConfigurationBridge;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.config.internal.Settings;

import java.io.IOException;

import static net.zyclonite.sonar.slacknotifier.extension.task.Analyses.PROJECT_KEY;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

public class SlackPostProjectAnalysisTaskTest {

    private static final String HOOK = "hook";

    CaptorPostProjectAnalysisTask postProjectAnalysisTask;
    SlackPostProjectAnalysisTask task;
    public Slack slackClient;
    private Settings settings;

    @Before
    public void before() throws IOException {
        postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();
        settings = new MapSettings();
        settings.setProperty(SlackNotifierProp.ENABLED.property(), "true");
        settings.setProperty(SlackNotifierProp.HOOK.property(), HOOK);
        settings.setProperty(SlackNotifierProp.CONFIG.property(), PROJECT_KEY);
        settings.setProperty(SlackNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + SlackNotifierProp.PROJECT.property(), PROJECT_KEY);
        settings.setProperty(SlackNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + SlackNotifierProp.QG_FAIL_ONLY.property(), "false");
        settings.setProperty("sonar.core.serverBaseURL", "http://your.sonar.com/");
        slackClient = Mockito.mock(Slack.class);
        WebhookResponse webhookResponse = WebhookResponse.builder().code(200).build();
        when(slackClient.send(anyString(), any(Payload.class))).thenReturn(webhookResponse);
        task = new SlackPostProjectAnalysisTask(slackClient, new ConfigurationBridge(settings));
    }

    @Test
    public void shouldCall() throws Exception {
        Analyses.simple(postProjectAnalysisTask);
        task.finished(new CaptorPostProjectAnalysisTask.ContextImpl(postProjectAnalysisTask.getProjectAnalysis(), null));
        Mockito.verify(slackClient, times(1)).send(eq(HOOK), any(Payload.class));
    }

    @Test
    public void shouldSkipIfPluginDisabled() {
        settings.setProperty(SlackNotifierProp.ENABLED.property(), "false");
        Analyses.simple(postProjectAnalysisTask);
        task.finished(new CaptorPostProjectAnalysisTask.ContextImpl(postProjectAnalysisTask.getProjectAnalysis(), null));
        Mockito.verifyZeroInteractions(slackClient);
    }

    @Test
    public void shouldSkipIfNoConfigFound() {
        Analyses.simpleDifferentKey(postProjectAnalysisTask);
        task.finished(new CaptorPostProjectAnalysisTask.ContextImpl(postProjectAnalysisTask.getProjectAnalysis(), null));
        Mockito.verifyZeroInteractions(slackClient);
    }

    @Test
    public void shouldSkipIfReportFailedQualityGateButOk() {
        settings.setProperty(SlackNotifierProp.CONFIG.property() + "." + PROJECT_KEY + "." + SlackNotifierProp.QG_FAIL_ONLY.property(), "true");
        Analyses.simple(postProjectAnalysisTask);
        task.finished(new CaptorPostProjectAnalysisTask.ContextImpl(postProjectAnalysisTask.getProjectAnalysis(), null));
        Mockito.verifyZeroInteractions(slackClient);
    }
}
