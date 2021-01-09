package io.github.marcocipriani01.telescopetouch.data;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.source.proto.SourceProto;

/**
 * Base class for converting (ASCII) text data files into protocol buffers.
 * This class writes out a human readable version of the
 * messages with place holders for the text strings.
 *
 * @author Brent Bryan
 */
public abstract class AbstractAsciiProtoWriter {

    private static final String NAME_DELIMITER = "[|]+";

    /**
     * Returns the AstronomicalSource associated with the given line, or null if
     * the line does not correspond to a valid AstronomicalSource.
     */
    protected abstract SourceProto.AstronomicalSourceProto getSourceFromLine(String line, int index);

    /**
     * Gets the list of string IDs for the object names (that is, of the
     * form R.string.foo).
     *
     * @param names pipe-separated object names
     */
    protected List<String> rKeysFromName(String names) {
        List<String> rNames = new ArrayList<>();
        for (String name : names.split(NAME_DELIMITER)) {
            rNames.add(name.replaceAll(" ", "_").toLowerCase());
        }
        return rNames;
    }

    protected SourceProto.GeocentricCoordinatesProto getCoords(float ra, float dec) {
        return SourceProto.GeocentricCoordinatesProto.newBuilder()
                .setRightAscension(ra)
                .setDeclination(dec)
                .build();
    }

    public SourceProto.AstronomicalSourcesProto readSources(BufferedReader in) throws IOException {
        SourceProto.AstronomicalSourcesProto.Builder builder = SourceProto.AstronomicalSourcesProto.newBuilder();

        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            SourceProto.AstronomicalSourceProto source = getSourceFromLine(line, builder.getSourceCount());

            if (source != null) {
                builder.addSource(source);
            }
        }

        return builder.build();
    }

    public void writeFiles(String prefix, SourceProto.AstronomicalSourcesProto sources) throws IOException {

    /*FileOutputStream out = null;
    try {
      out = new FileOutputStream(prefix + ".binary");
      sources.writeTo(out);
    } finally {
      Closeables.closeSilently(out);
    }*/

        try (PrintWriter writer = new PrintWriter(new FileWriter(prefix + ".ascii"))) {
            writer.append(sources.toString());
        }

        System.out.println("Successfully wrote " + sources.getSourceCount() + " sources.");
    }

    public void run(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.printf("Usage: %s <inputfile> <outputprefix>", this.getClass().getCanonicalName());
            System.exit(1);
        }
        args[0] = args[0].trim();
        args[1] = args[1].trim();

        System.out.println("Input File: " + args[0]);
        System.out.println("Output Prefix: " + args[1]);
        try (BufferedReader in = new BufferedReader(new FileReader(args[0]))) {
            writeFiles(args[1], readSources(in));
        }
    }
}