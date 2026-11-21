package ai.gradpath.service;

import java.util.List;

/**
 * Minimal RFC 4180 CSV writer. Cells that start with = + - @ are prefixed with an apostrophe so spreadsheet
 * programs never interpret exported text as formulas (CSV injection protection).
 */
public final class CsvWriter {

    private CsvWriter() { }

    public static String write(List<String> header, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder();
        line(sb, header.stream().map(h -> (Object) h).toList());
        rows.forEach(r -> line(sb, r));
        return sb.toString();
    }

    private static void line(StringBuilder sb, List<Object> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(cell(cells.get(i)));
        }
        sb.append("\r\n");
    }

    static String cell(Object value) {
        String v = value == null ? "" : String.valueOf(value);
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0 && !(value instanceof Number)) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
