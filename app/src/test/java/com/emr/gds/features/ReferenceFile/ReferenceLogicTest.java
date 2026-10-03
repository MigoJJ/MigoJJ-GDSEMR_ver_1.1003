package com.emr.gds.features.ReferenceFile;

import com.emr.gds.features.ReferenceFile.application.*;
import com.emr.gds.features.ReferenceFile.persistence.SqliteReferenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceLogicTest {
    @TempDir Path temp;

    @Test
    void csvRoundTripPreservesKoreanQuotesAndMultipleLines() throws Exception {
        ReferenceItem item = new ReferenceItem("의료,약물", "첫 줄\r\n\"둘째 줄\"\n세 번째", "guidelines/독감");
        var rows = ReferenceCsv.read(new StringReader("\uFEFFCategory,Contents,Directory Path\r\n"
                + ReferenceCsv.row(item) + "\r\n"));
        assertEquals(2, rows.size());
        assertArrayEquals(new String[]{item.getCategory(), item.getContents(), item.getDirectoryPath()}, rows.get(1));
    }

    @Test
    void malformedCsvFailsBeforeImport() {
        assertThrows(IOException.class, () -> ReferenceCsv.read(new StringReader("Medical,\"unfinished")));
        assertThrows(IOException.class, () -> ReferenceCsv.read(new StringReader("\"Medical\"bad,contents,path")));
    }

    @Test
    void csvPreservesTrailingEmptyFieldsAndNoFinalNewline() throws Exception {
        var rows = ReferenceCsv.read(new StringReader("a,b,\n\"\",\"\",\"\""));
        assertArrayEquals(new String[]{"a", "b", ""}, rows.get(0));
        assertArrayEquals(new String[]{"", "", ""}, rows.get(1));
    }

    @Test
    void pathsRejectTraversalAbsolutePathsAndEscapingLinks() throws Exception {
        Path base = Files.createDirectory(temp.resolve("base"));
        Path outside = Files.createDirectory(temp.resolve("outside"));
        assertTrue(ReferencePaths.resolve(base, "guidelines\\독감").isPresent());
        for (String path : new String[]{"../outside", "a/../../outside", "/etc", "C:\\docs", "\\\\server\\docs"}) {
            assertTrue(ReferencePaths.resolve(base, path).isEmpty(), path);
        }
        Files.createSymbolicLink(base.resolve("link"), outside);
        assertTrue(ReferencePaths.resolve(base, "link").isEmpty());
        assertTrue(ReferencePaths.resolve(base, "link/not-created").isEmpty());
        Files.createSymbolicLink(base.resolve("broken"), temp.resolve("missing"));
        assertTrue(ReferencePaths.resolve(base, "broken/file").isEmpty());
    }

    @Test
    void realRepositoryCrudAndLiteralSearch() {
        var repo = new SqliteReferenceRepository(temp.resolve("references.db"));
        ReferenceItem item = repo.save(new ReferenceItem("의료", "100% flu_test", "guidelines/flu"));
        repo.save(new ReferenceItem("Medical", "100 fluXtest", "other"));
        assertTrue(item.getId() > 0);
        assertEquals(1, repo.search("%", null).size());
        assertEquals(1, repo.search("_", null).size());
        assertEquals(1, repo.search(" FLU ", "의료").size());
        assertTrue(repo.existsByCategoryAndContents("의료", "100% flu_test", 0));
        assertFalse(repo.existsByCategoryAndContents("의료", "100% flu_test", item.getId()));
        item.setContents("updated");
        repo.save(item);
        assertEquals("updated", repo.findById(item.getId()).orElseThrow().getContents());
        repo.delete(item);
        assertTrue(repo.findById(item.getId()).isEmpty());
    }

    @Test
    void databaseFailuresAndMissingRowsAreReported() throws Exception {
        Path database = temp.resolve("references.db");
        var repo = new SqliteReferenceRepository(database);
        assertThrows(IllegalStateException.class, () -> repo.save(new ReferenceItem(99, "a", "b", "c")));
        assertThrows(IllegalStateException.class, () -> repo.delete(new ReferenceItem(99, "a", "b", "c")));
        Files.delete(database);
        Files.createDirectory(database);
        ReferenceItem unsaved = new ReferenceItem("a", "b", "c");
        assertThrows(IllegalStateException.class, () -> repo.save(unsaved));
        assertEquals(0, unsaved.getId());
        assertThrows(IllegalStateException.class, repo::findAll);
    }
}
