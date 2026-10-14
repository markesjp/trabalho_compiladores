package scanner;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Uso: java -cp out scanner.Main <arquivo.lumen>");
            System.exit(2);
        }

        String source = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8);
        Scanner scanner = new Scanner(source);
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
