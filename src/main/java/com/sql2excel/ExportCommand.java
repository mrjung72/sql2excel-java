package com.sql2excel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sql2excel.config.*;
import com.sql2excel.excel.ExcelExporter;
import com.sql2excel.query.QueryExecutor;
import com.sql2excel.query.QueryResult;
import com.sql2excel.variable.VariableResolver;
import picocli.CommandLine.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;

@Command(name = "export", description = "Execute queries and export results to Excel")
public class ExportCommand implements Callable<Integer> {

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
        String outputPath = new VariableResolver().resolve(excelConfig.getOutput(), vars);

        return ExportService.export(queryConfig, databases, vars, outputPath);
    }
}
