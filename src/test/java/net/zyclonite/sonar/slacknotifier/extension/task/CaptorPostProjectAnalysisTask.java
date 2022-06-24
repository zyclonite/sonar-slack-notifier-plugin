package net.zyclonite.sonar.slacknotifier.extension.task;

import org.sonar.api.ce.posttask.PostProjectAnalysisTask;

class CaptorPostProjectAnalysisTask implements PostProjectAnalysisTask {
    private ProjectAnalysis projectAnalysis;

    @Override
    public void finished(Context context) {
        this.projectAnalysis = context.getProjectAnalysis();
    }

    public ProjectAnalysis getProjectAnalysis() {
        return projectAnalysis;
    }

    public static class ContextImpl implements PostProjectAnalysisTask.Context {
        private final ProjectAnalysis projectAnalysis;
        private final CaptorPostProjectAnalysisTask task;

        public ContextImpl(ProjectAnalysis projectAnalysis, CaptorPostProjectAnalysisTask task) {
            this.projectAnalysis = projectAnalysis;
            this.task = task;
        }

        @Override
        public PostProjectAnalysisTask.ProjectAnalysis getProjectAnalysis() {
            return projectAnalysis;
        }

        @Override
        public LogStatistics getLogStatistics() {
            return null;
        }
    }
}
