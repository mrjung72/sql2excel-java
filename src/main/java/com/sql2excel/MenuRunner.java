package com.sql2excel;

import picocli.CommandLine;

import java.io.File;
import java.io.FileFilter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MenuRunner {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Scanner scanner;
    private final String version;

    public MenuRunner(String version) {
        this.scanner = new Scanner(System.in);
        this.version = version;
    }

    public void run() {
        while (true) {
            printHeader();
            printMenu();
            String selection = prompt("선택하세요 (0-5): ");
            if (selection == null) {
                break;
            }
            switch (selection.trim()) {
                case "0":
                    goodbye();
                    return;
                case "1":
                    validateQuery();
                    break;
                case "2":
                    testConnection();
                    break;
                case "3":
                    exportExcel(true);
                    break;
                case "4":
                    exportExcel(false);
                    break;
                case "5":
                    printHelp();
                    break;
                default:
                    System.out.println("잘못된 선택입니다. 다시 시도하세요.");
            }
        }
    }

    private void printHeader() {
        System.out.println("============================================================");
        System.out.println("  SQL2Excel v" + version);
        System.out.println("============================================================");
        System.out.println();
    }

    private void printMenu() {
        System.out.println("메뉴 선택");
        System.out.println();
        System.out.println("1. 쿼리 정의 파일 검증");
        System.out.println("2. 데이터베이스 연결 테스트");
        System.out.println();
        System.out.println("3. 엑셀 파일 생성 (XML)");
        System.out.println("4. 엑셀 파일 생성 (JSON)");
        System.out.println();
        System.out.println("5. 도움말");
        System.out.println("0. 종료");
        System.out.println();
    }

    private void validateQuery() {
        printHeader();
        System.out.println("쿼리 정의 파일 검증");
        System.out.println();

        FileInfo file = selectFile(null);
        if (file == null) {
            return;
        }

        System.out.println("쿼리 정의 파일을 검증하고 있습니다...");
        System.out.println();

        ValidateCommand command = new ValidateCommand();
        command.configPath = "config/dbinfo.json";
        if (file.path.toLowerCase().endsWith(".xml")) {
            command.xmlPath = file.path;
        } else {
            command.queryPath = file.path;
        }

        try {
            int code = command.call();
            if (code == 0) {
                System.out.println("✅ 쿼리 정의 파일 검증이 완료되었습니다.");
            } else {
                System.out.println("❌ 쿼리 정의 파일에 오류가 있습니다.");
            }
        } catch (Exception e) {
            System.out.println("❌ 쿼리 정의 파일에 오류가 있습니다.");
            System.out.println("오류: " + e.getMessage());
        }

        pause();
    }

    private void testConnection() {
        printHeader();
        System.out.println("데이터베이스 연결 테스트");
        System.out.println();
        System.out.println("데이터베이스 연결을 테스트하고 있습니다...");
        System.out.println();

        ListDbsCommand command = new ListDbsCommand();
        command.configPath = "config/dbinfo.json";
        int code = command.call();

        if (code == 0) {
            System.out.println("✅ 데이터베이스 연결 테스트가 성공했습니다.");
        } else {
            System.out.println("❌ 데이터베이스 연결에 실패했습니다.");
            System.out.println("config/dbinfo.json 파일의 연결 정보를 확인하세요.");
        }

        pause();
    }

    private void exportExcel(boolean xml) {
        printHeader();
        String type = xml ? "XML" : "JSON";
        System.out.println("엑셀 파일 생성 (" + type + ")");
        System.out.println();

        FileInfo file = selectFile(xml ? "xml" : "json");
        if (file == null) {
            return;
        }

        System.out.println("엑셀 파일을 생성하고 있습니다...");
        System.out.println();

        LocalDateTime startTime = LocalDateTime.now();

        ExportCommand command = new ExportCommand();
        command.configPath = "config/dbinfo.json";
        if (xml) {
            command.xmlPath = file.path;
        } else {
            command.queryPath = file.path;
        }

        try {
            int code = command.call();
            LocalDateTime endTime = LocalDateTime.now();
            if (code == 0) {
                System.out.println("✅ 엑셀 파일이 성공적으로 생성되었습니다.");
                System.out.println("시작 시간: " + startTime.format(TIME_FORMATTER));
                System.out.println("종료 시간: " + endTime.format(TIME_FORMATTER));
            } else {
                System.out.println("❌ 엑셀 파일 생성 중 오류가 발생했습니다.");
            }
        } catch (Exception e) {
            System.out.println("❌ 엑셀 파일 생성 중 오류가 발생했습니다.");
            System.out.println("오류: " + e.getMessage());
        }

        pause();
    }

    private void printHelp() {
        printHeader();
        System.out.println("도움말");
        System.out.println("SQL2Excel 도구 - SQL 쿼리 결과를 엑셀 파일로 보내기");
        System.out.println();
        System.out.println("주요 기능:");
        System.out.println("- 다중 데이터베이스 연결 지원");
        System.out.println("- XML 및 JSON 쿼리 정의 파일");
        System.out.println("- 쿼리 내 변수 치환");
        System.out.println("- 엑셀 스타일링 및 포맷팅");
        System.out.println("- 다중 시트 지원");
        System.out.println();
        System.out.println("사용 방법:");
        System.out.println("1. 검증: 쿼리 정의 파일 확인");
        System.out.println("2. 연결 테스트: 데이터베이스 연결 확인");
        System.out.println("3. 생성: 쿼리에서 엑셀 파일 생성");
        System.out.println();
        System.out.println("설정:");
        System.out.println("- 데이터베이스: config/dbinfo.json");
        System.out.println("- 쿼리: queries/ 폴더");
        System.out.println("- 출력: output/ 폴더");
        System.out.println();
        new CommandLine(new Sql2ExcelApplication()).usage(System.out);
        pause();
    }

    private FileInfo selectFile(String filterType) {
        File queriesDir = new File("queries");
        List<FileInfo> files = new ArrayList<>();

        if (queriesDir.exists() && queriesDir.isDirectory()) {
            File[] queryFiles = queriesDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File f) {
                    if (!f.isFile()) {
                        return false;
                    }
                    String name = f.getName().toLowerCase();
                    if (filterType == null) {
                        return name.endsWith(".xml") || name.endsWith(".json");
                    }
                    return name.endsWith("." + filterType);
                }
            });
            if (queryFiles != null) {
                Arrays.sort(queryFiles);
                for (File f : queryFiles) {
                    String name = f.getName().toLowerCase();
                    String type = name.endsWith(".xml") ? "XML" : "JSON";
                    files.add(new FileInfo(f.getName(), f.getPath(), type));
                }
            }
        }

        if (files.isEmpty()) {
            System.out.println("(쿼리 정의 파일이 없습니다)");
            System.out.println();
            pause();
            return null;
        }

        System.out.println("쿼리 정의 파일 선택");
        System.out.println("사용 가능한 쿼리 정의 파일:");
        System.out.println();
        for (int i = 0; i < files.size(); i++) {
            FileInfo file = files.get(i);
            System.out.println("  " + (i + 1) + ". " + file.name + " (" + file.type + ")");
        }
        System.out.println();

        String input = prompt("파일 번호 선택 (1-" + files.size() + "): ");
        if (input == null || input.isEmpty()) {
            System.out.println("파일 번호가 입력되지 않았습니다.");
            System.out.println();
            pause();
            return null;
        }

        int num;
        try {
            num = Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("잘못된 파일 번호입니다. 다음 범위에서 입력하세요 1-" + files.size());
            System.out.println();
            pause();
            return null;
        }

        if (num < 1 || num > files.size()) {
            System.out.println("잘못된 파일 번호입니다. 다음 범위에서 입력하세요 1-" + files.size());
            System.out.println();
            pause();
            return null;
        }

        FileInfo selected = files.get(num - 1);
        System.out.println("선택된 파일: " + selected.name);
        System.out.println();
        return selected;
    }

    private String prompt(String message) {
        System.out.print(message);
        System.out.flush();
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    private void pause() {
        System.out.println();
        System.out.println("계속하려면 아무 키나 누르세요...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }

    private void goodbye() {
        System.out.println();
        System.out.println("SQL2Excel 도구를 사용해주셔서 감사합니다!");
    }

    private static class FileInfo {
        final String name;
        final String path;
        final String type;

        FileInfo(String name, String path, String type) {
            this.name = name;
            this.path = path;
            this.type = type;
        }
    }
}

