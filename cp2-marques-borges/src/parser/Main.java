package parser;

import scanner.Scanner;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java -cp out parser.Main <arquivo.lumen>");
            System.exit(2);
        }
        try {
            String fonte = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
            Parser parser = new Parser(new Scanner(fonte));
            boolean aceito = parser.parse();
            parser.errosLexicos().forEach(System.err::println);
            parser.erros().forEach(System.err::println);
            System.out.println(aceito ? "Programa aceito." : "Programa rejeitado.");
            if (!aceito) System.exit(1);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Nao foi possivel ler o arquivo: " + e.getMessage());
            System.exit(2);
        }
    }
}
