package net.zyclonite.sonar.slacknotifier.extension.task;

import com.github.seratch.jslack.Slack;
import com.github.seratch.jslack.api.webhook.Payload;
import com.github.seratch.jslack.api.webhook.WebhookResponse;
import net.zyclonite.sonar.slacknotifier.common.component.AbstractSlackNotifyingComponent;
import net.zyclonite.sonar.slacknotifier.common.component.ProjectConfig;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.io.IOException;
import java.util.Optional;

public class SlackPostProjectAnalysisTask extends AbstractSlackNotifyingComponent implements PostProjectAnalysisTask {
    private static final Logger LOG = Loggers.get(SlackPostProjectAnalysisTask.class);

    private final Slack slackClient;

    public SlackPostProjectAnalysisTask(Configuration configuration) {
        this(Slack.getInstance(), configuration);
    }

    public SlackPostProjectAnalysisTask(Slack slackClient, Configuration configuration) {
        super(configuration);
        this.slackClient = slackClient;
    }

    @Override
    public void finished(Context context) {
        ProjectAnalysis analysis = context.getProjectAnalysis();
        refreshSettings();
        if (!isPluginEnabled()) {
            LOG.debug("Slack notifier plugin disabled, skipping. Settings are [{}]", logRelevantSettings());
            return;
        }
        LOG.debug("Analysis ScannerContext: [{}]", analysis.getScannerContext().getProperties());
        String projectKey = analysis.getProject().getKey();

        Optional<ProjectConfig> projectConfigOptional = getProjectConfig(projectKey);
        if (projectConfigOptional.isEmpty()) {
            return;
        }

        ProjectConfig projectConfig = projectConfigOptional.get();
        if (shouldSkipSendingNotification(projectConfig, analysis.getQualityGate())) {
            return;
        }

        LOG.debug("Slack notification will be sent: " + analysis);

        Payload payload = ProjectAnalysisPayloadBuilder.of(analysis)
            .projectConfig(projectConfig)
            .projectUrl(projectUrl(projectKey))
            .build();

        try {
            WebhookResponse response = slackClient.send(getSlackIncomingWebhookUrl(), payload);
            if (!Integer.valueOf(200).equals(response.getCode())) {
                LOG.error("Failed to post to slack, response is [{}]", response);
            }
        } catch (IOException e) {
            LOG.error("Failed to send slack message", e);
        }
    }

    private String projectUrl(String projectKey) {
        return getSonarServerUrl() + "dashboard?id=" + projectKey;
    }


}
