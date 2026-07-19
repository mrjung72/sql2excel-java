package com.sql2excel;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "sql2excel-java",
        mixinStandardHelpOptions = true,
        version = "1.0.0",
        description = "SQL query results to Excel exporter (Java version)",
        subcommands = {
                ExportCommand.class,
                ListDbsCommand.class,
                ValidateCommand.class
        }
)
public class Sql2ExcelApplication {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            new MenuRunner("1.0.0").run();
            return;
        }

        int exitCode = new CommandLine(new Sql2ExcelApplication())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .execute(args);
        System.exit(exitCode);
    }
}
