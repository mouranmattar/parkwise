package control;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import entity.AnnualSummarySource;
import entity.ClubGrowthRow;
import entity.ParkingSession;
import entity.ParkingLot;
import utils.DatabaseConnection;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class ReportController {

    private static final String[] JRXML_PATHS = {
        "/reports/AnnualSummaryReport.jrxml",
        "/reports/AnnualSummary.jrxml" // fallback if renamed
    };

    public void generateAnnualReport(int year, String pdfPath, String xmlPath) throws JRException {
        JasperReport report = loadPrecompiledReport();

        Map<String, Object> params = new HashMap<>();
        params.put("REPORT_YEAR", year);

        // Controller performs aggregation over raw entity data (no SQL aggregation)
        List<ParkingSession> sessions = DatabaseConnection.getSessionsByYear(year);
        Map<Integer, double[]> aggregates = new HashMap<>(); // lotId -> [totalAmount, otherCount]
        Map<Integer, String[]> lotMeta = new HashMap<>();    // lotId -> [name, city]
        Map<Integer, String> vehicleColorCache = new HashMap<>();

        for (ParkingSession session : sessions) {
            int lotId = session.getParkingLotID();
            ParkingLot lot = lotMeta.containsKey(lotId) ? null : DatabaseConnection.getParkingLotById(lotId);
            if (lot != null) {
                lotMeta.put(lotId, new String[]{lot.getName(), lot.getCity()});
            }

            double[] agg = aggregates.computeIfAbsent(lotId, k -> new double[]{0d, 0d});
            agg[0] += session.getFinalAmount();

            String color = vehicleColorCache.computeIfAbsent(
                    session.getVehicleNumber(),
                    v -> {
                        String c = DatabaseConnection.getVehicleColor(v);
                        return c == null ? "" : c;
                    }
            );
            if ("other".equalsIgnoreCase(color)) {
                agg[1] += 1d;
            }
        }

        List<AnnualSummarySource> data = new ArrayList<>();
        for (Map.Entry<Integer, double[]> entry : aggregates.entrySet()) {
            int lotId = entry.getKey();
            double[] agg = entry.getValue();
            String[] meta = lotMeta.get(lotId);
            String name = meta != null ? meta[0] : "Lot " + lotId;
            String city = meta != null ? meta[1] : "";
            data.add(new AnnualSummarySource(lotId, name, city, agg[0], Math.round(agg[1])));
        }

        data.sort(Comparator
                .comparing(AnnualSummarySource::getCity, String.CASE_INSENSITIVE_ORDER.reversed())
                .thenComparingDouble(AnnualSummarySource::getTotalAmount));

        JRBeanCollectionDataSource ds = new JRBeanCollectionDataSource(data);

        JasperPrint print;
        try {
            print = JasperFillManager.fillReport(report, params, ds);
        } catch (JRException fillEx) {
            // Fallback to freshly compiling JRXML in case bundled .jasper is outdated
            JasperReport compiled = compileFromJrxml();
            print = JasperFillManager.fillReport(compiled, params, ds);
        }

        if (pdfPath != null && !pdfPath.isEmpty()) {
            JasperExportManager.exportReportToPdfFile(print, pdfPath);
        }
        if (xmlPath != null && !xmlPath.isEmpty()) {
            JasperExportManager.exportReportToXmlFile(print, xmlPath, true);
        }
    }

    public List<ClubGrowthRow> getClubGrowthRows(int year) {
        int[] monthly = DatabaseConnection.getMonthlyClubJoins(year);
        int baseCumulative = DatabaseConnection.getClubMembersBeforeYear(year);
        List<ClubGrowthRow> rows = new ArrayList<>();
        int cumulative = baseCumulative;
        for (int i = 0; i < monthly.length; i++) {
            int month = i + 1;
            int newMembers = monthly[i];
            int prev = cumulative;
            cumulative += newMembers;
            double growth;
            if (i == 0) {
                // January: compare cumulative against baseCumulative
                if (baseCumulative == 0) {
                    growth = newMembers > 0 ? 100.0 : 0.0;
                } else {
                    growth = ((cumulative - baseCumulative) * 100.0) / baseCumulative;
                }
            } else {
                // Months 2-12: compare current cumulative to previous month cumulative
                if (prev == 0) {
                    growth = newMembers > 0 ? 100.0 : 0.0;
                } else {
                    growth = (newMembers * 100.0) / prev;
                }
            }
            rows.add(new ClubGrowthRow(month, newMembers, cumulative, growth));
        }
        return rows;
    }

    private JasperReport loadPrecompiledReport() throws JRException {
        String[] jasperPaths = {
            "/reports/AnnualSummaryReport.jasper",
            "/reports/AnnualSummary.jasper"
        };
        for (String path : jasperPaths) {
            InputStream in = ReportController.class.getResourceAsStream(path);
            if (in != null) {
                return (JasperReport) JRLoader.loadObject(in);
            }
        }
        throw new JRException("Missing precompiled Jasper file (.jasper) under /reports. Generate AnnualSummaryReport.jasper once and include it in the jar.");
    }

    private JasperReport compileFromJrxml() throws JRException {
        String[] jrxmlPaths = {
            "/reports/AnnualSummaryReport.jrxml",
            "/reports/AnnualSummary.jrxml"
        };
        for (String path : jrxmlPaths) {
            InputStream in = ReportController.class.getResourceAsStream(path);
            if (in != null) {
                return JasperCompileManager.compileReport(in);
            }
        }
        // Fallback to filesystem (useful when running outside jar or when classpath resources are stale)
        Path base = Paths.get(System.getProperty("user.dir"), "reports");
        Path[] filePaths = {
            base.resolve("AnnualSummaryReport.jrxml"),
            base.resolve("AnnualSummary.jrxml")
        };
        for (Path p : filePaths) {
            if (Files.exists(p)) {
                return JasperCompileManager.compileReport(p.toString());
            }
        }
        throw new JRException("JRXML not found under /reports for compilation fallback.");
    }
}
