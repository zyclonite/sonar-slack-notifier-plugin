package net.zyclonite.sonar.slacknotifier;

import net.zyclonite.sonar.slacknotifier.extension.task.SlackPostProjectAnalysisTask;
import net.zyclonite.sonar.slacknotifier.common.SlackNotifierProp;
import org.sonar.api.Plugin;
import org.sonar.api.PropertyType;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.config.PropertyFieldDefinition;

import java.util.ArrayList;
import java.util.List;

public class SlackNotifierPlugin implements Plugin {
    private static final String CATEGORY = "Slack";
    private static final String SUBCATEGORY = "Slack Notifier";

    @Override
    public void define(Context context) {
        List<Object> extensions = new ArrayList<>();

        addPluginPropertyDefinitions(extensions);

        extensions.add(SlackPostProjectAnalysisTask.class);

        context.addExtensions(extensions);
    }

    private void addPluginPropertyDefinitions(List<Object> extensions) {
        extensions.add(PropertyDefinition.builder(SlackNotifierProp.HOOK.property())
            .name("Slack web integration hook")
            .description("https://api.slack.com/incoming-webhooks")
            .type(PropertyType.STRING)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(0)
            .build());
        extensions.add(PropertyDefinition.builder(SlackNotifierProp.ENABLED.property())
            .name("Plugin enabled")
            .description("Are Slack notifications enabled in general?")
            .defaultValue("false")
            .type(PropertyType.BOOLEAN)
            .category(CATEGORY)
            .subCategory(SUBCATEGORY)
            .index(2)
            .build());

        extensions.add(
            PropertyDefinition.builder(SlackNotifierProp.CONFIG.property())
                .name("Project specific configuration")
                .description("Project specific configuration: Specify notification only on failing Quality Gate.")
                .category(CATEGORY)
                .subCategory(SUBCATEGORY)
                .index(3)
                .fields(
                    PropertyFieldDefinition.build(SlackNotifierProp.PROJECT.property())
                        .name("Project Key")
                        .description("Ex: net.zyclonite.sonar:sonar-slack-notifier-plugin, can use '*' wildcard at the end")
                        .type(PropertyType.STRING)
                        .build(),
                    PropertyFieldDefinition.build(SlackNotifierProp.QG_FAIL_ONLY.property())
                        .name("Send on failed Quality Gate")
                        .description("Should notification be sent only if Quality Gate did not pass OK")
                        .type(PropertyType.BOOLEAN)
                        .build()
                )
                .build());
    }
}
