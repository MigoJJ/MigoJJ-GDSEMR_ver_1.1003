package com.emr.gds.features.ReferenceFile.application;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public final class ReferenceCsv {
    private ReferenceCsv() {}

    public static List<String[]> read(Reader input) throws IOException {
        PushbackReader reader = new PushbackReader(input, 1);
        List<String[]> rows = new ArrayList<>();
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean closed = false;
        boolean first = true;
        int ch;
        while ((ch = reader.read()) != -1) {
            if (first && ch == '\uFEFF') { first = false; continue; }
            first = false;
            if (quoted) {
                if (ch == '"') {
                    int next = reader.read();
                    if (next == '"') field.append('"');
                    else {
                        quoted = false;
                        closed = true;
                        if (next != -1) reader.unread(next);
                    }
                } else field.append((char) ch);
            } else if (ch == ',' || ch == '\n' || ch == '\r') {
                fields.add(field.toString());
                field.setLength(0);
                closed = false;
                if (ch != ',') {
                    rows.add(fields.toArray(String[]::new));
                    fields.clear();
                    if (ch == '\r') {
                        int next = reader.read();
                        if (next != '\n' && next != -1) reader.unread(next);
                    }
                }
            } else if (ch == '"' && field.isEmpty() && !closed) quoted = true;
            else {
                if (closed || ch == '"') throw new IOException("Invalid CSV quoting.");
                field.append((char) ch);
            }
        }
        if (quoted) throw new IOException("Unterminated CSV field.");
        if (!fields.isEmpty() || !field.isEmpty() || closed) {
            fields.add(field.toString());
            rows.add(fields.toArray(String[]::new));
        }
        return rows;
    }

    public static String row(ReferenceItem item) {
        return escape(item.getCategory()) + "," + escape(item.getContents()) + "," + escape(item.getDirectoryPath());
    }

    private static String escape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
