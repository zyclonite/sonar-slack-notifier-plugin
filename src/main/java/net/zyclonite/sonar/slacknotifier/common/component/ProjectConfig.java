package net.zyclonite.sonar.slacknotifier.common.component;

import net.zyclonite.sonar.slacknotifier.common.SlackNotifierProp;
import org.sonar.api.config.Configuration;

import java.util.Objects;

public class ProjectConfig {
    private final String projectKey;
    private final boolean qgFailOnly;

    public ProjectConfig(String projectKey, boolean qgFailOnly) {
        this.projectKey = projectKey;
        this.qgFailOnly = qgFailOnly;
    }

    public ProjectConfig(ProjectConfig c) {
        this.projectKey = c.getProjectKey();
        this.qgFailOnly = c.isQgFailOnly();
    }

    static ProjectConfig create(Configuration configuration, String configurationId) {
        String configurationPrefix = SlackNotifierProp.CONFIG.property() + "." + configurationId + ".";
        String projectKey = configuration.get(configurationPrefix + SlackNotifierProp.PROJECT.property()).orElse(null);
        boolean qgFailOnly = configuration.getBoolean(configurationPrefix + SlackNotifierProp.QG_FAIL_ONLY.property()).orElse(false);
        return new ProjectConfig(projectKey, qgFailOnly);
    }

    public String getProjectKey() {
        return projectKey;
    }

    public boolean isQgFailOnly() {
        return qgFailOnly;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectConfig that = (ProjectConfig) o;
        return qgFailOnly == that.qgFailOnly &&
            Objects.equals(projectKey, that.projectKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectKey, qgFailOnly);
    }

    @Override
    public String toString() {
        return "ProjectConfig{" + "projectKey='" + projectKey + '\'' +
            ", qgFailOnly=" + qgFailOnly +
            '}';
    }
}
