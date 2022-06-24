package net.zyclonite.sonar.slacknotifier.common.component;

import net.zyclonite.sonar.slacknotifier.common.SlackNotifierProp;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.config.Configuration;
import org.sonar.api.utils.MessageException;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractSlackNotifyingComponent {

    private static final Logger LOG = Loggers.get(AbstractSlackNotifyingComponent.class);

    private final Configuration configuration;
    private Map<String, ProjectConfig> projectConfigMap = Collections.emptyMap();

    protected AbstractSlackNotifyingComponent(Configuration configuration) {
        this.configuration = configuration;
        LOG.info("Constructor called, project config map is constructed from general settings");
    }

    protected void refreshSettings() {
        LOG.debug("Refreshing settings");
        refreshProjectConfigs();
    }

    private void refreshProjectConfigs() {
        LOG.debug("Refreshing project configs");
        Set<ProjectConfig> oldValues = new HashSet<>();
        this.projectConfigMap.values().forEach(c -> oldValues.add(new ProjectConfig(c)));
        this.projectConfigMap = buildProjectConfigByProjectKeyMap(configuration);
        Set<ProjectConfig> newValues = new HashSet<>(this.projectConfigMap.values());
        if (!oldValues.equals(newValues)) {
            LOG.debug("Old configs [{}] --> new configs [{}]", oldValues, newValues);
        }
    }

    protected String getSlackIncomingWebhookUrl() {
        return configuration.get(SlackNotifierProp.HOOK.property()).orElse(null);
    }

    protected boolean isPluginEnabled() {
        return configuration.getBoolean(SlackNotifierProp.ENABLED.property()).orElse(false);
    }

    protected String getSonarServerUrl() {
        String u = configuration.get("sonar.core.serverBaseURL").orElse(null);
        if (u == null) {
            return null;
        }
        if (u.endsWith("/")) {
            return u;
        }
        return u + "/";
    }

    protected Optional<ProjectConfig> getProjectConfig(String projectKey) {
        List<ProjectConfig> projectConfigs = projectConfigMap.keySet()
            .stream()
            .filter(key -> key.endsWith("*") ? projectKey.startsWith(key.substring(0, key.length() - 1))
                : key.equals(projectKey))
            .map(projectConfigMap::get)
            .collect(Collectors.toList());
        // Not configured at all
        if (projectConfigs.isEmpty()) {
            LOG.info("Could not find config for project [{}] in [{}]", projectKey, projectConfigMap);
            return Optional.empty();
        }

        if (projectConfigs.size() > 1) {
            LOG.warn("More than 1 project key was matched. Using first one: {}", projectConfigs.get(0).getProjectKey());
        }
        return Optional.of(projectConfigs.get(0));
    }

    private static Map<String, ProjectConfig> buildProjectConfigByProjectKeyMap(Configuration configuration) {
        Map<String, ProjectConfig> map = new HashMap<>();
        String[] projectConfigIndexes = configuration.getStringArray(SlackNotifierProp.CONFIG.property());
        for (String projectConfigIndex : projectConfigIndexes) {
            String projectKeyProperty = SlackNotifierProp.CONFIG.property() + "." + projectConfigIndex + "." + SlackNotifierProp.PROJECT.property();
            String projectKey = configuration.get(projectKeyProperty).orElse(null);
            if (projectKey == null) {
                throw MessageException.of("Slack notifier configuration is corrupted. At least one project specific parameter has no project key. " +
                    "Contact your administrator to update this configuration in the global administration section of SonarQube.");
            }
            ProjectConfig value = ProjectConfig.create(configuration, projectConfigIndex);
            LOG.debug("Found project configuration [{}]", value);
            map.put(projectKey, value);
        }
        return map;
    }

    protected String logRelevantSettings() {
        Map<String, String> pluginSettings = new HashMap<>();
        mapSetting(pluginSettings, SlackNotifierProp.HOOK);
        mapSetting(pluginSettings, SlackNotifierProp.ENABLED);
        mapSetting(pluginSettings, SlackNotifierProp.CONFIG);
        return pluginSettings + "; project specific config: " + projectConfigMap;
    }

    private void mapSetting(Map<String, String> pluginSettings, SlackNotifierProp key) {
        pluginSettings.put(key.name(), configuration.get(key.property()).orElse(null));
    }

    protected boolean shouldSkipSendingNotification(ProjectConfig projectConfig, QualityGate qualityGate) {
        if (projectConfig.isQgFailOnly() && qualityGate != null && QualityGate.Status.OK.equals(qualityGate.getStatus())) {
            LOG.info("Project [{}] set up to send notification on failed Quality Gate, but was: {}", projectConfig.getProjectKey(), qualityGate.getStatus().name());
            return true;
        }
        return false;
    }
}
