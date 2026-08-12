package com.sql2excel;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Callable;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;

@Command(name = "format-xml", description = "Pretty-print an XML file")
public class FormatXmlCommand implements Callable<Integer> {

    @Option(names = {"-i", "--input"}, description = "Input XML file path", required = true)
    String inputPath;

    @Option(names = {"-o", "--output"}, description = "Output XML file path (default: print to stdout)")
    String outputPath;

    @Override
    public Integer call() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setCoalescing(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(Paths.get(inputPath).toFile());

            DOMImplementation impl = doc.getImplementation();
            DOMImplementationLS implLS = (DOMImplementationLS) impl.getFeature("LS", "3.0");
            LSSerializer serializer = implLS.createLSSerializer();
            serializer.getDomConfig().setParameter("format-pretty-print", true);
            serializer.getDomConfig().setParameter("xml-declaration", true);

            java.io.StringWriter writer = new java.io.StringWriter();
            LSOutput output = implLS.createLSOutput();
            output.setCharacterStream(writer);
            output.setEncoding(StandardCharsets.UTF_8.name());
            serializer.write(doc, output);
            String formatted = writer.toString();

            if (outputPath != null && !outputPath.isEmpty()) {
                Files.writeString(Paths.get(outputPath), formatted, StandardCharsets.UTF_8);
                System.out.println("Formatted XML written to: " + outputPath);
            } else {
                System.out.print(formatted);
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            return 1;
        }
    }
}
