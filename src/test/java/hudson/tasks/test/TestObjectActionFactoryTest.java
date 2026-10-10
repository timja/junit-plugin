package hudson.tasks.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.junit.CaseResult;
import hudson.tasks.junit.ClassResult;
import hudson.tasks.junit.JUnitResultArchiver;
import hudson.tasks.junit.TestResultAction;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import jenkins.model.menu.Group;
import org.htmlunit.html.DomElement;
import org.htmlunit.html.HtmlPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestBuilder;
import org.jvnet.hudson.test.TestExtension;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class TestObjectActionFactoryTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule rule) {
        j = rule;
    }

    @Test
    void actionsAreRenderedInTheAppBarOfEveryTestPage() throws Exception {
        FreeStyleBuild build = buildWithTestResults();
        hudson.tasks.junit.TestResult result =
                build.getAction(TestResultAction.class).getResult();
        ClassResult classResult = result.byPackage("org.twia.vendor").getClassResult("VendorManagerTest");
        CaseResult caseResult = classResult.getChildren().iterator().next();

        // Only the server-rendered markup is checked; the history page's scripts are beyond HtmlUnit
        JenkinsRule.WebClient wc = j.createWebClient().withJavaScriptEnabled(false);

        HtmlPage report = wc.getPage(build, "testReport");
        String reportActions = appBarTemplate(report);
        assertThat(reportActions, containsString("Analyze"));
        assertThat("menu actions are not buttons", reportActions, not(containsString("Menu entry")));
        assertThat(reportActions, not(containsString("Class only")));
        assertThat(overflowButton(report).getAttribute("class"), not(containsString("jenkins-hidden")));

        HtmlPage classPage = wc.goTo(classResult.getUrl());
        String classActions = appBarTemplate(classPage);
        assertThat(classActions, containsString("Analyze"));
        assertThat(classActions, containsString("Class only"));
        assertThat(
                "nothing for the menu",
                overflowButton(classPage).getAttribute("class"),
                containsString("jenkins-hidden"));

        assertThat(appBarTemplate(wc.goTo(caseResult.getUrl())), containsString("Analyze"));
        assertThat(appBarTemplate(wc.goTo(caseResult.getUrl() + "/history")), containsString("Analyze"));
    }

    @Test
    void overflowMenuOnlyContainsMenuActions() throws Exception {
        FreeStyleBuild build = buildWithTestResults();
        JenkinsRule.WebClient wc = j.createWebClient();

        String menu = wc.goTo(build.getUrl() + "testReport/contextMenu?menu-only=true", "application/json")
                .getWebResponse()
                .getContentAsString();
        assertThat(menu, containsString("Menu entry"));
        assertThat(menu, not(containsString("Analyze")));

        String contextMenu = wc.goTo(build.getUrl() + "testReport/contextMenu", "application/json")
                .getWebResponse()
                .getContentAsString();
        assertThat(contextMenu, containsString("Menu entry"));
        assertThat(contextMenu, containsString("Analyze"));
    }

    @Test
    void actionsAreOrderedByGroupAndBoundToTheirUrl() throws Exception {
        FreeStyleBuild build = buildWithTestResults();
        hudson.tasks.junit.TestResult result =
                build.getAction(TestResultAction.class).getResult();

        List<Action> actions = result.getAppBarActions();
        assertThat(actions.get(0).getDisplayName(), is("Analyze"));
        assertThat(actions.get(1).getDisplayName(), is("Menu entry"));

        assertThat(result.getDynamic("analyze", null, null), sameInstance(actions.get(0)));
        assertThat(result.getDynamic("unknown", null, null), nullValue());
    }

    private static String appBarTemplate(HtmlPage page) {
        DomElement template = page.getFirstByXPath(
                "//div[contains(@class, 'jenkins-app-bar__controls')]//button[@data-type='auto-overflow']/following-sibling::template");
        return template.asXml();
    }

    private static DomElement overflowButton(HtmlPage page) {
        return page.getFirstByXPath(
                "//div[contains(@class, 'jenkins-app-bar__controls')]//button[@data-type='auto-overflow']");
    }

    private FreeStyleBuild buildWithTestResults() throws Exception {
        FreeStyleProject p = j.createFreeStyleProject();
        p.getBuildersList().add(new TestBuilder() {
            @Override
            public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener)
                    throws InterruptedException, IOException {
                build.getWorkspace()
                        .child("junit.xml")
                        .copyFrom(getClass().getResource("/hudson/tasks/junit/junit-report-20090516.xml"));
                return true;
            }
        });
        p.getPublishersList().add(new JUnitResultArchiver("*.xml"));
        return j.assertBuildStatus(Result.UNSTABLE, p.scheduleBuild2(0).get());
    }

    private static Action action(String displayName, String urlName, Group group) {
        return new Action() {
            @Override
            public String getIconFileName() {
                return "symbol-analytics-outline plugin-ionicons-api";
            }

            @Override
            public String getDisplayName() {
                return displayName;
            }

            @Override
            public String getUrlName() {
                return urlName;
            }

            @Override
            public Group getGroup() {
                return group;
            }
        };
    }

    @TestExtension
    public static class EveryTestObject extends TestObjectActionFactory<TestResult> {
        private Action analyze;
        private Action menuEntry;

        @Override
        public Class<TestResult> type() {
            return TestResult.class;
        }

        @Override
        public Collection<? extends Action> createFor(TestResult target) {
            // Cache so tests can compare identity across calls
            if (analyze == null) {
                analyze = action("Analyze", "analyze", Group.IN_APP_BAR);
                menuEntry = action("Menu entry", "menu-entry", Group.IN_MENU);
            }
            if (target instanceof hudson.tasks.junit.TestResult) {
                // Deliberately out of order to check sorting
                return List.of(menuEntry, analyze);
            }
            return List.of(analyze);
        }
    }

    @TestExtension
    public static class ClassesOnly extends TestObjectActionFactory<ClassResult> {
        @Override
        public Class<ClassResult> type() {
            return ClassResult.class;
        }

        @Override
        public Collection<? extends Action> createFor(ClassResult target) {
            return List.of(action("Class only", "class-only", Group.LAST_IN_APP_BAR));
        }
    }
}
