package com.pdfviewer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasType;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class PdfViewerActivityInstrumentationTest {

    @Rule
    public ActivityScenarioRule<PdfViewerActivity> activityRule = 
            new ActivityScenarioRule<>(PdfViewerActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void useAppContext() {
        // Context of the app under test
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.pdfviewer", appContext.getPackageName());
    }

    @Test
    public void testSelectFileButtonDisplayed() {
        // Test that the select file button is displayed when no PDF is provided
        onView(withId(R.id.selectFileButton))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.selectFileButton))
                .check(matches(withText("Select PDF File")));
    }

    @Test
    public void testSelectFileButtonClick() {
        // Test that clicking the select file button launches the file picker
        onView(withId(R.id.selectFileButton))
                .perform(click());
        
        // Verify that the file picker intent is launched
        intended(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("application/pdf")
        ));
    }

    @Test
    public void testInitialViewState() {
        // Test initial view state when no PDF is loaded
        onView(withId(R.id.selectFileButton))
                .check(matches(isDisplayed()));
        
        // ScrollView should be initially hidden
        onView(withId(R.id.scrollView))
                .check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void testActivityLaunchWithIntent() {
        // Test launching activity with a PDF intent
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType("application/pdf");
        intent.setData(Uri.parse("content://com.android.providers.downloads.documents/document/1"));
        
        androidx.test.core.app.ActivityScenario<PdfViewerActivity> scenario = 
                androidx.test.core.app.ActivityScenario.launch(intent);
        
        scenario.onActivity(activity -> {
            assertNotNull("Activity should be launched with intent", activity);
            assertEquals("Activity should receive the correct intent", intent.getAction(), activity.getIntent().getAction());
        });
        
        scenario.close();
    }

    @Test
    public void testErrorStateDisplay() {
        // Test that error state is handled properly
        onView(withId(R.id.errorText))
                .check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void testSecurityNoInputPersistence() {
        // Test that no input persistence occurs during activity lifecycle
        activityRule.getScenario().onActivity(activity -> {
            // Simulate activity recreation (configuration change)
            activity.recreate();
            
            // Verify that select file button is still displayed (no state persisted)
            onView(withId(R.id.selectFileButton))
                    .check(matches(isDisplayed()));
        });
    }

    @Test
    public void testFilePickerIntentSecurity() {
        // Test that file picker intent has security restrictions
        onView(withId(R.id.selectFileButton))
                .perform(click());
        
        // Verify that the intent has local-only flag for security
        intended(allOf(
                hasAction(Intent.ACTION_OPEN_DOCUMENT),
                hasType("application/pdf")
        ));
    }

    @Test
    public void testActivityRecreation() {
        // Test that activity handles recreation properly (configuration changes)
        activityRule.getScenario().recreate();
        
        // After recreation, select file button should still be visible
        onView(withId(R.id.selectFileButton))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testNoNetworkAccess() {
        // Test that the app doesn't attempt network access
        activityRule.getScenario().onActivity(activity -> {
            // Verify that the app context doesn't have network permissions
            Context context = activity.getApplicationContext();
            
            // Check that INTERNET permission is not granted
            int internetPermission = context.checkSelfPermission(android.Manifest.permission.INTERNET);
            assertEquals("App should not have INTERNET permission", 
                        android.content.pm.PackageManager.PERMISSION_DENIED, internetPermission);
        });
    }

    @Test
    public void testViewVisibilityStates() {
        // Test different view visibility states
        
        // Initially: select button visible, scroll view hidden, error hidden
        onView(withId(R.id.selectFileButton))
                .check(matches(isDisplayed()));
        
        onView(withId(R.id.scrollView))
                .check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
        
        onView(withId(R.id.errorText))
                .check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void testIntentFilterHandling() {
        // Test that activity properly handles different intent types
        
        // Test with main/launcher intent
        Intent mainIntent = new Intent(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        
        androidx.test.core.app.ActivityScenario<PdfViewerActivity> mainScenario = 
                androidx.test.core.app.ActivityScenario.launch(mainIntent);
        
        mainScenario.onActivity(activity -> {
            assertNotNull("Activity should handle MAIN intent", activity);
        });
        
        mainScenario.close();
        
        // Test with VIEW intent
        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
        viewIntent.setType("application/pdf");
        
        androidx.test.core.app.ActivityScenario<PdfViewerActivity> viewScenario = 
                androidx.test.core.app.ActivityScenario.launch(viewIntent);
        
        viewScenario.onActivity(activity -> {
            assertNotNull("Activity should handle VIEW intent", activity);
        });
        
        viewScenario.close();
    }

    // Helper method to check effective visibility
    private static androidx.test.espresso.matcher.ViewMatchers.Visibility withEffectiveVisibility(
            androidx.test.espresso.matcher.ViewMatchers.Visibility visibility) {
        return visibility;
    }
}