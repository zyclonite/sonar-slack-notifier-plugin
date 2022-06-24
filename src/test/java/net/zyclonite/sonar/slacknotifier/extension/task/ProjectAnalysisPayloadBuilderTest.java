package net.zyclonite.sonar.slacknotifier.extension.task;

import com.github.seratch.jslack.api.model.Attachment;
import com.github.seratch.jslack.api.model.Field;
import com.github.seratch.jslack.api.webhook.Payload;
import net.zyclonite.sonar.slacknotifier.common.component.ProjectConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectAnalysisPayloadBuilderTest {
    private static final boolean QG_FAIL_ONLY = true;
    CaptorPostProjectAnalysisTask postProjectAnalysisTask;
    Locale defaultLocale;

    @Before
    public void before() {
        postProjectAnalysisTask = new CaptorPostProjectAnalysisTask();

        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void after() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void execute_is_passed_a_non_null_ProjectAnalysis_object() {
        Analyses.simple(postProjectAnalysisTask);
        assertThat(postProjectAnalysisTask.getProjectAnalysis()).isNotNull();
    }

    @Test
    public void testPayloadBuilder() {
        Analyses.qualityGateOk4Conditions(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", false);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .build();
        assertThat(payload).isEqualTo(expected());
    }

    private Payload expected() {
        List<Attachment> attachments = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        fields.add(Field.builder()
            .title("New Vulnerabilities: OK")
            .value("0, error if >0")
            .valueShortEnough(false)
            .build());
        fields.add(Field.builder()
            .title("New Bugs: ERROR")
            .value("1, error if >0")
            .valueShortEnough(false)
            .build());
        fields.add(Field.builder()
            .title("Technical Debt Ratio on New Code: OK")
            .value("0.01%, error if >10.0%")
            .valueShortEnough(false)
            .build());
        fields.add(Field.builder()
            .title("Coverage on New Code: ERROR")
            .value("75.51%, error if <80.0%")
            .valueShortEnough(false)
            .build());

        attachments.add(Attachment.builder()
            .fields(fields)
            .color("good")
            .build());
        return Payload.builder()
            .text("Project [Project Name] analyzed. See "
                + "http://localhost:9000/dashboard?id=project:key. Quality gate status: OK")
            .attachments(attachments)
            .build();
    }

    @Test
    public void shouldShowOnlyExceededConditionsIfProjectConfigReportOnlyOnFailedQualityGateWay() {
        Analyses.qualityGateError2Of3ConditionsFailed(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", QG_FAIL_ONLY);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .build();

        assertThat(payload.getAttachments())
            .hasSize(1)
            .flatExtracting(Attachment::getFields)
            .hasSize(2)
            .extracting(Field::getTitle)
            .contains("Functions: ERROR", "Issues: ERROR");
    }

    @Test
    public void buildPayloadWithoutQualityGateWay() {
        Analyses.noQualityGate(postProjectAnalysisTask);
        ProjectConfig projectConfig = new ProjectConfig("key", false);
        Payload payload = ProjectAnalysisPayloadBuilder.of(postProjectAnalysisTask.getProjectAnalysis())
            .projectConfig(projectConfig)
            .projectUrl("http://localhost:9000/dashboard?id=project:key")
            .build();

        assertThat(payload.getAttachments()).isNull();
        assertThat(payload.getText()).doesNotContain("Quality Gate status");
    }
}
