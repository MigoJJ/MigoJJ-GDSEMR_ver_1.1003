package com.emr.gds.features.glp1.adapter.in.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class Glp1InsuranceReferenceUiTest {
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final AtomicReference<Path> opened = new AtomicReference<>();
    private volatile boolean launchOnFxThread;
    private volatile boolean fail;
    private Button reference;
    private Stage stage;

    @Start
    void start(Stage stage) {
        this.stage = stage;
        new Glp1SemaglutideMain() {
            @Override
            protected void openReferenceDocument(Path document) throws IOException, InterruptedException {
                opened.set(document);
                launchOnFxThread = Platform.isFxApplicationThread();
                started.countDown();
                if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Test opener timed out");
                if (fail) throw new IOException("Test launcher failed");
            }
        }.start(stage);
        reference = (Button) stage.getScene().lookup("#ozempicInsuranceReference");
    }

    @Test
    void opensExactDocumentWithoutBlockingTheGlp1Window(FxRobot robot) throws Exception {
        robot.interact(reference::fire);
        assertTrue(started.await(3, TimeUnit.SECONDS));
        assertFalse(launchOnFxThread);
        assertTrue(reference.isDisabled());
        assertTrue(opened.get().endsWith(Path.of("app", "db", "references", "guidelines",
                "Ozempic® (semaglutide) 한국 건강보험 급여 기준.odt")));
        // A stalled document launcher must not prevent subsequent FX work.
        robot.interact(() -> stage.setTitle("GLP-1 remains responsive"));
        assertEquals("GLP-1 remains responsive", stage.getTitle());
        release.countDown();
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !reference.isDisabled());
    }

    @Test
    void failedLaunchShowsAnErrorAndAllowsRetry(FxRobot robot) throws Exception {
        fail = true;
        release.countDown();
        robot.interact(reference::fire);
        assertTrue(started.await(3, TimeUnit.SECONDS));
        WaitForAsyncUtils.waitFor(5, TimeUnit.SECONDS, () -> !reference.isDisabled());
        robot.interact(() -> {
            var errors = new ArrayList<>(Window.getWindows()).stream()
                    .filter(w -> w != stage && w.isShowing())
                    .filter(w -> w.getScene().getRoot() instanceof DialogPane).toList();
            assertEquals(1, errors.size());
            DialogPane dialog = (DialogPane) errors.getFirst().getScene().getRoot();
            assertTrue(dialog.getContentText().contains("Test launcher failed"));
            errors.forEach(Window::hide);
        });
    }
}
