package net.zyclonite.sonar.slacknotifier.extension.task;

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.github.seratch.jslack.api.webhook.Payload;
import net.zyclonite.sonar.slacknotifier.common.component.ProjectConfig;
import org.sonar.api.ce.posttask.PostProjectAnalysisTask;
import org.sonar.api.ce.posttask.QualityGate;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

public class ProjectAnalysisPayloadBuilder {
    private static final Logger LOG = Loggers.get(ProjectAnalysisPayloadBuilder.class);
    private static final Map<String, String> METRIC_MAPPINGS = Map.of(
        "new_reliability_rating", "Reliability Rating on New Code",
        "new_maintainability_rating", "Maintainability Rating on New Code",
        "new_security_hotspots_reviewed", "Security Hotspots Reviewed on New Code",
        "new_sqale_debt_ratio", "Technical Debt Ratio on New Code",
        "new_vulnerabilities", "New Vulnerabilities",
        "new_bugs", "New Bugs",
        "new_coverage", "Coverage on New Code",
        "new_duplicated_lines_density", "Duplicated Lines (%) on New Code",
        "functions", "Functions",
        "violations", "Issues"
    );

    private static final String SLACK_GOOD_COLOUR = "good";
    private static final String SLACK_DANGER_COLOUR = "danger";
    private static final Map<QualityGate.Status, String> statusToColor = new EnumMap<>(QualityGate.Status.class);

    static {
        statusToColor.put(QualityGate.Status.OK, SLACK_GOOD_COLOUR);
        statusToColor.put(QualityGate.Status.ERROR, SLACK_DANGER_COLOUR);
    }

    PostProjectAnalysisTask.ProjectAnalysis analysis;
    private ProjectConfig projectConfig;
    private String projectUrl;

    private final DecimalFormat percentageFormat;

    private ProjectAnalysisPayloadBuilder(PostProjectAnalysisTask.ProjectAnalysis analysis) {
        this.analysis = analysis;
        this.percentageFormat = new DecimalFormat();
        this.percentageFormat.setMaximumFractionDigits(2);
    }

    public static ProjectAnalysisPayloadBuilder of(PostProjectAnalysisTask.ProjectAnalysis analysis) {
        return new ProjectAnalysisPayloadBuilder(analysis);
    }

    public ProjectAnalysisPayloadBuilder projectConfig(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
        return this;
    }

    public ProjectAnalysisPayloadBuilder projectUrl(String projectUrl) {
        this.projectUrl = projectUrl;
        return this;
    }

    public Payload build() {
        assertNotNull(projectConfig, "projectConfig");
        assertNotNull(projectUrl, "projectUrl");
        assertNotNull(analysis, "analysis");

        QualityGate qualityGate = analysis.getQualityGate();
        String shortText = String.join("",
            "Project [", analysis.getProject().getName(), "] analyzed. See ",
            projectUrl,
            qualityGate == null ? "." : ". Quality gate status: " + qualityGate.getStatus());

        return Payload.builder()
            .text(shortText)
            .attachments(qualityGate == null ? null : buildConditionsAttachment(qualityGate, projectConfig.isQgFailOnly()))
            .build();
    }

    private void assertNotNull(Object object, String argumentName) {
        if (object == null) {
            throw new IllegalArgumentException("[Assertion failed] - " + argumentName + " argument is required; it must not be null");
        }
    }

    private List<Attachment> buildConditionsAttachment(QualityGate qualityGate, boolean qgFailOnly) {
        List<Attachment> attachments = new ArrayList<>();
        attachments.add(Attachment.builder()
            .fields(
                qualityGate.getConditions()
                    .stream()
                    .filter(condition -> !qgFailOnly || notOkNorNoValue(condition))
                    .map(this::translate)
                    .collect(Collectors.toList()))
            .color(statusToColor.get(qualityGate.getStatus()))
            .build());
        return attachments;
    }

    private boolean notOkNorNoValue(QualityGate.Condition condition) {
        return !(QualityGate.EvaluationStatus.OK.equals(condition.getStatus())
            || QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus()));
    }

    private Field translate(QualityGate.Condition condition) {
        String conditionName = METRIC_MAPPINGS.getOrDefault(condition.getMetricKey(), condition.getMetricKey());

        if (QualityGate.EvaluationStatus.NO_VALUE.equals(condition.getStatus())) {
            return Field.builder().title(conditionName)
                .value(condition.getStatus().name())
                .valueShortEnough(true)
                .build();
        } else {
            StringBuilder sb = new StringBuilder();
            appendValue(condition, sb);
            appendValuePostfix(condition, sb);
            if (condition.getErrorThreshold() != null) {
                sb.append(", error if ");
                appendValueOperatorPrefix(condition, sb);
                sb.append(condition.getErrorThreshold());
                appendValuePostfix(condition, sb);
            }
            return Field.builder().title(conditionName + ": " + condition.getStatus().name())
                .value(sb.toString())
                .valueShortEnough(false)
                .build();
        }
    }

    private void appendValue(QualityGate.Condition condition, StringBuilder sb) {
        if ("".equals(condition.getValue())) {
            sb.append("-");
        } else {
            if (valueIsPercentage(condition)) {
                appendPercentageValue(condition.getValue(), sb);
            } else {
                sb.append(condition.getValue());
            }
        }
    }

    private void appendPercentageValue(String s, StringBuilder sb) {
        try {
            Double d = Double.parseDouble(s);
            sb.append(percentageFormat.format(d));
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse [{}] into a Double due to [{}]", s, e.getMessage());
            sb.append(s);
        }
    }

    private void appendValueOperatorPrefix(QualityGate.Condition condition, StringBuilder sb) {
        switch (condition.getOperator()) {
            case GREATER_THAN:
                sb.append(">");
                break;
            case LESS_THAN:
                sb.append("<");
                break;
            default:
                LOG.error("Unsupported operator");
                break;
        }
    }

    private void appendValuePostfix(QualityGate.Condition condition, StringBuilder sb) {
        if (valueIsPercentage(condition)) {
            sb.append("%");
        }
    }

    private boolean valueIsPercentage(QualityGate.Condition condition) {
        switch (condition.getMetricKey()) {
            case CoreMetrics.NEW_COVERAGE_KEY:
            case CoreMetrics.NEW_SQALE_DEBT_RATIO_KEY:
                return true;
            default:
                return false;
        }
    }
}
