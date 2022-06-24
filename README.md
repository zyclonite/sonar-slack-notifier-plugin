# Sonar Slack Notifier Plugin
SonarQube plugin for sending notifications to Slack

This plugin sends a Slack message of project analysis outcome to the configured slack channel.
The plugin uses Incoming Web Hook as the integration mechanism with Slack.

# Install
The plugin must be placed in *SONAR_HOME/extensions/downloads* directory and SonarQube must be restarted.

## Using latest release
You can find the latest release from https://github.com/zyclonite/sonar-slack-notifier-plugin/releases/ page.

## Build from sources
To build the plugin simply run
```
mvn clean package
```
