package scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        boolean tracing = args.length == 2 && "--trace".equals(args[0]);
        if (args.length != 1 && !tracing) {
            System.err.println("Uso: java -cp out scanner.Main [--trace] <arquivo.lumen>");
            System.exit(2);
            return;
        }

        String source;
        try {
            source = Files.readString(Path.of(args[tracing ? 1 : 0]), StandardCharsets.UTF_8);
        } catch (IOException | InvalidPathException error) {
            System.err.println("Não foi possível ler o arquivo: " + error.getMessage());
            System.exit(2);
            return;
        }

        Scanner scanner = tracing ? new Scanner(source, message -> System.err.println("[AFD] " + message))
                : new Scanner(source);
        Token token;
        do {
            token = scanner.nextToken();
            System.out.println(token);
        } while (token.tipo() != TipoToken.EOF);

        for (ErrorLexico error : scanner.errors()) {
            System.err.println(error);
        }
        if (!scanner.errors().isEmpty()) {
            System.exit(1);
        }
    }
}
