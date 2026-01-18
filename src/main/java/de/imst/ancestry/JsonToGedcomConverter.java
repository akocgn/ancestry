package de.imst.ancestry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class JsonToGedcomConverter {

    public static void main(String[] args) throws IOException {
        Path output = Path.of("familie.ged");
        ObjectMapper mapper = new ObjectMapper();
        StringBuilder gedcom = new StringBuilder();

        try (final InputStream is = JsonToGedcomConverter.class.getClassLoader().getResourceAsStream("familie.json")) {
            if (is == null) {
                throw new IllegalStateException("Resource 'familie.json' not found on classpath");
            }
            JsonNode root = mapper.readTree(is);

            // Header (minimal GEDCOM 5.5.1)
            gedcom.append("0 HEAD\n");
            gedcom.append("1 SOUR JSON2GED\n");
            gedcom.append("1 GEDC\n");
            gedcom.append("2 VERS 5.5.1\n");
            gedcom.append("2 FORM LINEAGE-LINKED\n");
            gedcom.append("1 CHAR UTF-8\n");

            // Individuals
            JsonNode individuals = root.get("individuals");
            if (individuals != null && individuals.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = individuals.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode indi = entry.getValue();
                    if (!indi.has("id")) {
                        System.err.println("Missing attribute 'id' for individual, skipping '"+indi+".");
                        continue;
                    }
                    String id = indi.get("id").asText();

                    gedcom.append("0 @").append(id).append("@ INDI\n");

                    // Name
                    if (indi.has("name")) {
                        String name = indi.get("name").asText();
                        // Sehr einfach: Nachname als letztes Wort
                        String[] parts = name.split(" ");
                        String surname = parts[parts.length - 1];
                        String given = name.substring(0, name.length() - surname.length()).trim();
                        gedcom.append("1 NAME ")
                                .append(given).append(" /").append(surname).append("/\n");
                    }

                    // Sex
                    if (indi.has("sex")) {
                        String sex = indi.get("sex").asText();
                        gedcom.append("1 SEX ").append(sex).append("\n");
                    }

                    // Events
                    JsonNode events = indi.get("events");
                    if (events != null && events.isObject()) {
                        // Birth
                        JsonNode birth = events.get("birth");
                        if (birth != null && birth.isObject()) {
                            gedcom.append("1 BIRT\n");
                            if (birth.has("date")) {
                                gedcom.append("2 DATE ")
                                        .append(birth.get("date").asText())
                                        .append("\n");
                            }
                            if (birth.has("place")) {
                                gedcom.append("2 PLAC ")
                                        .append(birth.get("place").asText())
                                        .append("\n");
                            }
                        }
                        // Death
                        JsonNode death = events.get("death");
                        if (death != null && death.isObject()) {
                            gedcom.append("1 DEAT\n");
                            if (death.has("date")) {
                                gedcom.append("2 DATE ")
                                        .append(death.get("date").asText())
                                        .append("\n");
                            }
                            if (death.has("place")) {
                                gedcom.append("2 PLAC ")
                                        .append(death.get("place").asText())
                                        .append("\n");
                            }
                        }
                    }

                    // Family links as spouse
                    JsonNode famAsSpouse = indi.get("families_as_spouse");
                    if (famAsSpouse != null && famAsSpouse.isArray()) {
                        for (JsonNode famId : famAsSpouse) {
                            gedcom.append("1 FAMS @")
                                    .append(famId.asText())
                                    .append("@\n");
                        }
                    }

                    // Family links as child
                    JsonNode famAsChild = indi.get("families_as_child");
                    if (famAsChild != null && famAsChild.isArray()) {
                        for (JsonNode famId : famAsChild) {
                            gedcom.append("1 FAMC @")
                                    .append(famId.asText())
                                    .append("@\n");
                        }
                    }
                }
            }

            // Families
            JsonNode families = root.get("families");
            if (families != null && families.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = families.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> entry = it.next();
                    JsonNode fam = entry.getValue();
                    String id = fam.get("id").asText();

                    gedcom.append("0 @").append(id).append("@ FAM\n");

                    if (fam.has("husband")) {
                        gedcom.append("1 HUSB @")
                                .append(fam.get("husband").asText())
                                .append("@\n");
                    }
                    if (fam.has("wife")) {
                        gedcom.append("1 WIFE @")
                                .append(fam.get("wife").asText())
                                .append("@\n");
                    }

                    JsonNode children = fam.get("children");
                    if (children != null && children.isArray()) {
                        for (JsonNode childId : children) {
                            gedcom.append("1 CHIL @")
                                    .append(childId.asText())
                                    .append("@\n");
                        }
                    }

                    // Optional: Familienereignisse (z.B. marriage)
                    JsonNode events = fam.get("events");
                    if (events != null && events.isObject()) {
                        JsonNode marriage = events.get("marriage");
                        if (marriage != null && marriage.isObject()) {
                            gedcom.append("1 MARR\n");
                            if (marriage.has("date")) {
                                gedcom.append("2 DATE ")
                                        .append(marriage.get("date").asText())
                                        .append("\n");
                            }
                            if (marriage.has("place")) {
                                gedcom.append("2 PLAC ")
                                        .append(marriage.get("place").asText())
                                        .append("\n");
                            }
                        }
                    }
                }
            }

            // Trailer
            gedcom.append("0 TRLR\n");
        }
        Files.writeString(output, gedcom.toString());
        System.out.println("GEDCOM written to " + output);
    }
}