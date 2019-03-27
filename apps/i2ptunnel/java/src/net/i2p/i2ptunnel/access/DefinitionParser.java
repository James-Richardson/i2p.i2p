package net.i2p.i2ptunnel.access;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

class DefinitionParser {

    static FilterDefinition parse(File f) throws IOException, InvalidDefinitionException {
        
        DefinitionBuilder builder = new DefinitionBuilder();

        BufferedReader reader = new BufferedReader(new FileReader(f));
        try {
            String line;
            while((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty())
                    continue;
                if (line.startsWith("#"))
                    continue;

                String [] split = line.split(" \t");
                split[0] = split[0].toLowerCase();
                if ("default".equals(split[0])) 
                    builder.setDefaultThreshold(parseThreshold(line.substring(7).trim()));
                else if ("recorder".equals(split[0]))
                    builder.addRecorder(parseRecorder(line.substring(8).trim()));
                else
                    builder.addElement(parseElement(line));
            }
        } finally {
            if (reader != null) try { reader.close(); } catch (IOException ignored) {}
        }

        return builder.build();
    }

    private static Threshold parseThreshold(String s) throws InvalidDefinitionException {
        if ("allow".equalsIgnoreCase(s))
            return Threshold.ALLOW;
        if ("deny".equalsIgnoreCase(s))
            return Threshold.DENY;

        String [] split = s.split("/");
        if (split.length != 2)
            throw new InvalidDefinitionException("Invalid threshold " + s);

        try {
            int connections = Integer.parseInt(split[0]);
            int minutes = Integer.parseInt(split[1]);
            if (connections < 0)
                throw new InvalidDefinitionException("Number of connections cannot be negative " + s);
            if (minutes < 1)
                throw new InvalidDefinitionException("Number of minutes must be at least 1 " + s);
            return new Threshold(connections, minutes);
        } catch (NumberFormatException bad) {
            throw new InvalidDefinitionException("Invalid threshold", bad);
        }
    }

    private static Recorder parseRecorder(String line) throws InvalidDefinitionException {
        String thresholdString = extractThreshold(line);

        Threshold threshold = parseThreshold(thresholdString);

        String line2 = line.substring(thresholdString.length()).trim();
        if (line2.length() == 0)
            throw new InvalidDefinitionException("Invalid definition "+ line);
        File file = new File(line2);
        return new Recorder(file, threshold);
    }

    private static String extractThreshold(String line) {
        StringBuilder thresholdBuilder = new StringBuilder();
        int i = 0;
        while(i < line.length()) {
            char c = line.charAt(i);
            if (c == ' ' || c == '\t')
                break;
            i++;
            thresholdBuilder.append(c);
        }
        return thresholdBuilder.toString();
    }

    private static FilterDefinitionElement parseElement(String line) throws InvalidDefinitionException {
        String thresholdString = extractThreshold(line);
        Threshold threshold = parseThreshold(thresholdString);
        String line2 = line.substring(thresholdString.length()).trim();
        String[] split = line2.split(" \t");
        if (split.length < 2)
            throw new InvalidDefinitionException("invalid definition "+line);
        if ("explicit".equalsIgnoreCase(split[0]))
            return new ExplicitFilterDefinitionElement(split[1], threshold);
        if ("file".equalsIgnoreCase(split[0])) {
            String line3 = line2.substring(4).trim();
            File file = new File(line3);
            return new FileFilterDefinitionElement(file, threshold);
        }
        throw new InvalidDefinitionException("invalid definition "+line);
    }

    private static class DefinitionBuilder {
        private Threshold threshold;
        private List<FilterDefinitionElement> elements = new ArrayList<FilterDefinitionElement>();
        private List<Recorder> recorders = new ArrayList<Recorder>();

        void setDefaultThreshold(Threshold threshold) throws InvalidDefinitionException {
            if (this.threshold != null)
                throw new InvalidDefinitionException("default already set!");
            this.threshold = threshold;
        }

        void addElement(FilterDefinitionElement element) {
            elements.add(element);
        }

        void addRecorder(Recorder recorder) {
            recorders.add(recorder);
        }

        FilterDefinition build() {
            if (threshold == null)
                threshold = Threshold.ALLOW;

            FilterDefinitionElement [] elArray = new FilterDefinitionElement[elements.size()];
            elArray = elements.toArray(elArray);

            Recorder [] rArray = new Recorder[recorders.size()];
            rArray = recorders.toArray(rArray);

            return new FilterDefinition(threshold, elArray, rArray);
        }
    }
}