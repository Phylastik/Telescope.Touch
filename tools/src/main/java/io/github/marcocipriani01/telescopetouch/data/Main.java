package io.github.marcocipriani01.telescopetouch.data;

import java.io.IOException;

/**
 * A frontend to the the various writers, since gradle appears unable to create separate
 * application distributions despite documentation to the contrary.
 * Usage:
 * java io.github.marcocipriani01.telescopetouch.data.Main <command> <args>
 * <p>
 * where command is one of GenStars, GenMessier, Binary
 * See the various writer classes for the args.
 */
public class Main {
    public static void main(String[] in) throws IOException {
        if (in.length < 2) {
            throw new IllegalArgumentException(
                    "Usage: java io.github.marcocipriani01.telescopetouch.data.Main <command> <args>");
        }
        Command command = Command.valueOf(in[0]);
        String[] args = new String[in.length - 1];
        System.arraycopy(in, 1, args, 0, in.length - 1);
        switch (command) {
            case GenStars:
                StellarAsciiProtoWriter.main(args);
                break;
            case GenMessier:
                MessierAsciiProtoWriter.main(args);
                break;
            case Binary:
                AsciiToBinaryProtoWriter.main(args);
                break;
            default:
                throw new IllegalArgumentException("Unknown command");
        }
    }

    private enum Command {
        GenStars, GenMessier, Rewrite, Binary
    }
}
