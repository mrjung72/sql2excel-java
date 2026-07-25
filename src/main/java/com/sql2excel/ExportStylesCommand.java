package com.sql2excel;

import com.sql2excel.config.ConfigLoader;
import com.sql2excel.config.DatabaseConfig;
import com.sql2excel.config.ExcelConfig;
import com.sql2excel.config.QueryConfig;
import com.sql2excel.config.SheetConfig;
import com.sql2excel.config.StyleTemplate;
import com.sql2excel.variable.VariableResolver;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(name = "export-styles", description = "Execute queries and export results to Excel with every defined style")
public class ExportStylesCommand implements Callable<Integer> {

    @Option(names = {"-c", "--config"}, description = "DB config file path", defaultValue = "config/dbinfo.json")
    String configPath;

    @Option(names = {"-q", "--query"}, description = "Query definition JSON file path")
    String queryPath;

    @Option(names = {"-x", "--xml"}, description = "Query definition XML file path")
    String xmlPath;

    @Option(names = {"-v", "--var"}, description = "Variables in key=value format (repeatable)", mapFallbackValue = Option.NULL_VALUE)
    Map<String, String> cliVars;

    @Override
    public Integer call() throws Exception {
        if ((queryPath == null || queryPath.isEmpty()) && (xmlPath == null || xmlPath.isEmpty())) {
            System.err.println("Error: either --query or --xml must be specified.");
            return 1;
        }

        Path queryFile = Paths.get(queryPath != null ? queryPath : xmlPath);
        Path dbConfigFile = Paths.get(configPath);

        ConfigLoader loader = new ConfigLoader();
        Map<String, DatabaseConfig> databases = loader.loadDatabaseConfig(dbConfigFile);
        QueryConfig queryConfig = loader.loadQueryConfig(queryFile);

        ExcelConfig excelConfig = queryConfig.getExcel();
        if (excelConfig == null || excelConfig.getOutput() == null || excelConfig.getOutput().isEmpty()) {
            System.err.println("Error: output path is required in excel config.");
            return 1;
        }

        Map<String, Object> vars = ExportService.buildVariables(queryConfig.getVars(), cliVars);
        String baseOutput = new VariableResolver().resolve(excelConfig.getOutput(), vars);

        List<String> styleNames = StyleTemplate.getStyleNames();
        if (styleNames.isEmpty()) {
            System.err.println("Error: no styles found in config/excel-style.json");
            return 1;
        }

        int failures = 0;
        for (String styleName : styleNames) {
            applyStyle(queryConfig, styleName);
            String outputPath = appendStyleSuffix(baseOutput, styleName);
            System.out.println("[" + styleName + "] Exporting to: " + outputPath);
            try {
                ExportService.export(queryConfig, databases, vars, outputPath);
            } catch (Exception e) {
                System.err.println("[" + styleName + "] Export failed: " + e.getMessage());
                failures++;
            }
        }

        return failures == 0 ? 0 : 1;
    }

    private void applyStyle(QueryConfig queryConfig, String styleName) {
        Map<String, Object> header = copy(StyleTemplate.getStyleHeader(styleName));
        Map<String, Object> body = copy(StyleTemplate.getStyleBody(styleName));

        ExcelConfig excelConfig = queryConfig.getExcel();
        if (excelConfig != null) {
            excelConfig.setHeader(header);
            excelConfig.setBody(body);
        }

        if (queryConfig.getSheets() != null) {
            for (SheetConfig sheet : queryConfig.getSheets()) {
                sheet.setHeader(header);
                sheet.setBody(body);
            }
        }
    }

    private Map<String, Object> copy(Map<String, Object> map) {
        return map == null ? null : new HashMap<>(map);
    }

    private String appendStyleSuffix(String outputPath, String styleName) {
        Path path = Paths.get(outputPath);
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String name = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        return path.resolveSibling(name + "-" + styleName + ext).toString();
    }
}
