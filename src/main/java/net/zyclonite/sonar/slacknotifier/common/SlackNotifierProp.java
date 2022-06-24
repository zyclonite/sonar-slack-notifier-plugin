package net.zyclonite.sonar.slacknotifier.common;

public enum SlackNotifierProp {

    HOOK("slacknotifier.hook"),

    ENABLED("slacknotifier.enabled"),

    CONFIG("slacknotifier.projectconfig"),
    /**
     * @see SlackNotifierProp#CONFIG
     */
    PROJECT("project"),
    /**
     * @see SlackNotifierProp#CONFIG
     */
    QG_FAIL_ONLY("qg");

    private String property;

    SlackNotifierProp(String property) {
        this.property = property;
    }

    public String property() {
        return property;
    }
}
