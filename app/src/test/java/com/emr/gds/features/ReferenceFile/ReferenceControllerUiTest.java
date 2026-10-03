package com.emr.gds.features.ReferenceFile;

import com.emr.gds.features.ReferenceFile.adapter.in.ui.ReferenceController;
import com.emr.gds.features.ReferenceFile.application.ReferenceItem;
import com.emr.gds.features.ReferenceFile.application.ReferenceService;
import com.emr.gds.features.ReferenceFile.persistence.SqliteReferenceRepository;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import java.nio.file.Path;
import java.nio.file.Files;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
class ReferenceControllerUiTest {
    @TempDir Path temp;
    private ReferenceController controller;
    private TableView<ReferenceItem> table;
    private TextField search;
    private ComboBox<String> category;
    private SqliteReferenceRepository repository;

    @Start
    @SuppressWarnings("unchecked")
    void start(Stage stage) throws Exception {
        repository = new SqliteReferenceRepository(temp.resolve("references.db"));
        repository.save(new ReferenceItem("Medical", "Influenza", "guidelines/flu"));
        repository.save(new ReferenceItem("Lab", "Blood count", "labs"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_frame.fxml"));
        stage.setScene(new Scene(loader.load()));
        controller = loader.getController();
        controller.setBasePath(Files.createDirectories(temp.resolve("base")).toFile());
        controller.setReferenceService(new ReferenceService(repository));
        controller.initData();
        stage.show();
        table = (TableView<ReferenceItem>) loader.getNamespace().get("referenceTable");
        search = (TextField) loader.getNamespace().get("searchField");
        category = (ComboBox<String>) loader.getNamespace().get("categoryFilter");
    }

    @Test
    void realFxmlSearchTrimsAndFiltersAllThreeFields(FxRobot robot) {
        assertEquals(2, table.getItems().size());
        robot.interact(() -> { search.setText(" FLU "); search.fireEvent(new ActionEvent()); });
        assertEquals(1, table.getItems().size());
        assertEquals("Influenza", table.getItems().getFirst().getContents());
        robot.interact(() -> { search.setText("labs"); search.fireEvent(new ActionEvent()); });
        assertEquals(1, table.getItems().size());
        assertEquals("Blood count", table.getItems().getFirst().getContents());
    }

    @Test
    void reloadingPreservesCategoryAndEmptyDatabaseHasNoFakeRows(FxRobot robot) throws Exception {
        var reload = ReferenceController.class.getDeclaredMethod("reloadDataFromDb");
        reload.setAccessible(true);
        robot.interact(() -> {
            category.setValue("Medical");
            try { reload.invoke(controller); } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals("Medical", category.getValue());
        assertEquals(1, table.getItems().size());
        robot.interact(() -> {
            repository.findAll().forEach(repository::delete);
            controller.initData();
        });
        assertTrue(table.getItems().isEmpty());
    }

    @Test
    void findUsesSelectedFilesReferenceFolder(FxRobot robot) throws Exception {
        Path selected = temp.resolve("base/guidelines/flu/document.docx");
        Files.createDirectories(selected.getParent());
        Files.writeString(selected, "test selection");
        var find = ReferenceController.class.getDeclaredMethod("findReferencesForFile", java.io.File.class);
        find.setAccessible(true);
        robot.interact(() -> {
            category.setValue("Lab");
            try { find.invoke(controller, selected.toFile()); } catch (Exception e) { throw new RuntimeException(e); }
        });
        assertEquals("All", category.getValue());
        assertEquals(1, table.getItems().size());
        assertEquals("Influenza", table.getItems().getFirst().getContents());
    }

    @Test
    void editDraftNeverMutatesOriginalBeforePersistence(FxRobot robot) {
        ReferenceItem original = new ReferenceItem(5, "Medical", "Original", "guidelines");
        robot.interact(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/reference_item_edit.fxml"));
                loader.load();
                ReferenceItemEditController editor = loader.getController();
                editor.setBasePath(temp.resolve("base").toFile());
                editor.setReferenceItem(original);
                ((TextField) loader.getNamespace().get("contentsField")).setText("Changed");
                ReferenceItem draft = editor.getReferenceItem();
                assertNotSame(original, draft);
                assertEquals(5, draft.getId());
                assertEquals("Changed", draft.getContents());
                assertEquals("Original", original.getContents());
            } catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
