package com.example.rdfgenerator;

import com.github.javafaker.Faker;
import com.mifmif.common.regex.Generex;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.vocabulary.SHACLM;
import org.apache.jena.vocabulary.RDF;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class CreateRDFData {


    public static void main(String[] args) {
        String fileName = "turtleShapes/shAndExampleShape.ttl";
        Integer numberOfExamples= 1;

        for (int i = 0; i < numberOfExamples; i++) {
            Model generatedData = generateDataModel(fileName);
            generatedData.write(System.out, "Turtle");

            // Validation on generated data
            Graph shapesGraph = RDFDataMgr.loadGraph(fileName);
            Shapes shapes = Shapes.parse(shapesGraph);
            ValidationReport report = ShaclValidator.get().validate(shapes, generatedData.getGraph());

            // Check if the data conforms to the shape
            if (report.conforms()) {
                System.out.println("Data conforms to the SHACL shape.");
            } else {
                // Print validation errors and details
                report.getModel().write(System.out, "Turtle");
            }
        }
    }
private static Model generateDataModel(String fileName) {
    Model shapeModel = ModelFactory.createDefaultModel();
    RDFDataMgr.read(shapeModel, fileName, Lang.TURTLE);

    // Create an RDF Model for data
    Model dataModel = ModelFactory.createDefaultModel();

    // Define the namespace prefixes
    String exNs = "http://example.org/";

    // Try to iterate over the shape model using different types of shapes
    ResIterator shapeIterator = shapeModel.listResourcesWithProperty(RDF.type, SHACLM.Shape);

    // If no shapes with RDF.type SHACLM.Shape, try NodeShape
    if (!shapeIterator.hasNext()) {
        shapeIterator = shapeModel.listResourcesWithProperty(RDF.type, SHACLM.NodeShape);
    }

    // If still no shapes, try PropertyShape
    if (!shapeIterator.hasNext()) {
        shapeIterator = shapeModel.listResourcesWithProperty(RDF.type, SHACLM.PropertyShape);
    }

    // Iterate over the matching resources
    while (shapeIterator.hasNext()) {
        Resource shapeResource  = shapeIterator.nextResource();

        // Check if the shape is deactivated
        if (isShapeDeactivated(shapeResource)) {
            System.out.println("Shape is deactivated. Skipping data generation for this shape.");
            continue; // Skip generating data for this shape
        }

        // Apply constraints defined within the sh:Shape
        applyShapeConstraints(dataModel, shapeResource, exNs);
    }

    return dataModel;
}

    private static void applyShapeConstraints(Model dataModel, Resource shapeResource, String exNs) {
        // Create a resource based on the shape's target class
        String targetName;
        if (shapeResource.getProperty(SHACLM.targetClass)!= null) {
            String url = shapeResource.getProperty(SHACLM.targetClass).getObject().toString();
            String[] parts = url.split("/");
            targetName = parts[parts.length - 1];
        }
        else if(shapeResource.getURI() != null){
            String url = shapeResource.getURI().toString();
            String[] parts = url.split("/");
            targetName = parts[parts.length - 1];
        } else {
            targetName = "Name"; // Default value if targetClass is not specified
        }
            Resource dataResource = dataModel.createResource(exNs + targetName+ "Resource");
            if(shapeResource.getProperty(SHACLM.targetClass) != null)
                 dataResource.addProperty(RDF.type, shapeResource.getProperty(SHACLM.targetClass).getObject());


            // Iterate over the shape's properties
            StmtIterator propertyIterator = shapeResource.listProperties(SHACLM.property);
            while (propertyIterator.hasNext()) {
                Statement propertyStatement = propertyIterator.nextStatement();
                Resource propertyResource = propertyStatement.getObject().asResource();

                // Check if the property is deactivated
                if (isShapeDeactivated(propertyResource)) {
                    System.out.println("Property is deactivated. Skipping data generation for this property.");
                    continue; // Skip generating data for this property
                }

                // Check if sh:not constraint is set
                if (propertyResource.hasProperty(SHACLM.not)) {
                    // Skip generating this property because sh:not is set
                    System.out.println("sh:not constraint is set for this property. Skipping data generation.");
                    continue;
                }

                // Extract property path and datatype information
                Property propertyPath = propertyResource.getProperty(SHACLM.path).getObject().as(Property.class);
                RDFNode datatypeNode = propertyResource.getProperty(SHACLM.datatype) != null
                        ? propertyResource.getProperty(SHACLM.datatype).getObject()
                        : null;

            // Property Pair Constraint Components: equals, disjoint, lessThan
                Property equals = propertyResource.getProperty(SHACLM.equals) != null
                        ? propertyResource.getProperty(SHACLM.equals).getObject().as(Property.class)
                        : null; // Get sh:equals constraint if present

                Property disjoint = propertyResource.getProperty(SHACLM.disjoint) != null
                        ? propertyResource.getProperty(SHACLM.disjoint).getObject().as(Property.class)
                        : null; // Get sh:disjoint constraint if present


                // Check if there's a minCount constraint
                int minCount = propertyResource.getProperty(SHACLM.minCount) != null
                        ? propertyResource.getProperty(SHACLM.minCount).getInt()
                        : 1; // Default to 1 if no minCount is specified

                int maxCount = propertyResource.getProperty(SHACLM.maxCount) != null
                        ? propertyResource.getProperty(SHACLM.maxCount).getInt()
                        : -1; // Default to -1 if no maxCount is specified

                int randomCount = maxCount == -1
                        ? minCount
                        : minCount + new Random().nextInt(maxCount - minCount + 1);

                // Generate values and apply the sh:equals constraint if it exists
                for (int i = 0; i < randomCount; i++) {
                    RDFNode generatedValue = generateValue(propertyResource);

                    if (generatedValue != null) {
                        dataResource.addProperty(propertyPath, generatedValue);

                        // Check for the sh:equals constraint
                        if (equals != null) {
                            dataResource.addProperty(equals, generatedValue);
                        }
                        if(disjoint != null){
                            if (!isPropertyPresent(dataModel, dataResource, disjoint)) {
//                                System.out.println("The property is not present for the resource.");
                                RDFNode generatedValueDisjoint = generateValue(propertyResource);
                                        //generateLiteralValue(null,null,null,null,null,null,null);
                                if(generatedValueDisjoint.equals(generatedValue)){
                                   generatedValueDisjoint = generateValue(propertyResource);
                                }
                                dataResource.addProperty(disjoint, generatedValueDisjoint);
                            }
                            dataResource.addProperty(propertyPath, generatedValue);

                        }
                    } else {
                        dataResource.addProperty(propertyPath, "Default Value");
                    }
                }

            }
//        }
    }

    public static boolean isPropertyPresent(Model model, Resource subject, Property property) {
        StmtIterator stmtIterator = model.listStatements(subject, property, (RDFNode) null);
        return stmtIterator.hasNext();
    }
    // Function to check if a shape is deactivated
    private static boolean isShapeDeactivated(Resource shapeResource) {
        Statement deactivatedStatement = shapeResource.getProperty(SHACLM.deactivated);
        if (deactivatedStatement != null) {
            RDFNode deactivatedValue = deactivatedStatement.getObject();
            if (deactivatedValue.isLiteral() && deactivatedValue.asLiteral().getBoolean()) {
                return true; // Shape is deactivated
            }
        }
        return false; // Shape is not deactivated or sh:deactivated is not set
    }


    private static RDFNode getNodeKindConstraint(Resource propertyResource) {
        if (propertyResource.hasProperty(SHACLM.nodeKind)) {
            return propertyResource.getProperty(SHACLM.nodeKind).getObject();
        }
        return null;
    }

    private static RDFNode getDatatypeConstraint(Resource propertyResource) {
        if (propertyResource.hasProperty(SHACLM.datatype)) {
            return propertyResource.getProperty(SHACLM.datatype).getObject();
        }
        return null;
    }

    // Function to generate values based on the datatype constraint
    private static RDFNode generateValue(Resource propertyResource) {
        RDFNode expectedNodeKind = getNodeKindConstraint(propertyResource);
        RDFNode datatypeConstraint = getDatatypeConstraint(propertyResource);


        // Retrieve minimum and maximum constraints
        Literal minExclusive = propertyResource.getProperty(SHACLM.minExclusive) != null
                ? propertyResource.getProperty(SHACLM.minExclusive).getObject().asLiteral()
                : null;
        Literal minInclusive = propertyResource.getProperty(SHACLM.minInclusive) != null
                ? propertyResource.getProperty(SHACLM.minInclusive).getObject().asLiteral()
                : null;
        Literal maxExclusive = propertyResource.getProperty(SHACLM.maxExclusive) != null
                ? propertyResource.getProperty(SHACLM.maxExclusive).getObject().asLiteral()
                : null;
        Literal maxInclusive = propertyResource.getProperty(SHACLM.maxInclusive) != null
                ? propertyResource.getProperty(SHACLM.maxInclusive).getObject().asLiteral()
                : null;


        // Retrieve minLength and maxLength constraints
        Literal minLength = propertyResource.getProperty(SHACLM.minLength) != null
                ? propertyResource.getProperty(SHACLM.minLength).getObject().asLiteral()
                : null;
        Literal maxLength = propertyResource.getProperty(SHACLM.maxLength) != null
                ? propertyResource.getProperty(SHACLM.maxLength).getObject().asLiteral()
                : null;

        //pathName za randomString
        String pathName = "";
        if(propertyResource.getProperty(SHACLM.path) !=null){
            String pathNameUnsplitted = propertyResource.getProperty(SHACLM.path).getObject().toString();
            String[] parts = pathNameUnsplitted.split("/");
            pathName = parts[parts.length - 1];
        }

        if (expectedNodeKind != null ) {
            if (expectedNodeKind.equals(SHACLM.IRI)) {
                // Generate a random IRI
                return ResourceFactory.createResource("http://example.org/" + generateRandomString(pathName,minLength,maxLength));
            } else if (expectedNodeKind.equals(SHACLM.Literal)) {
                return generateLiteralValue(pathName,datatypeConstraint, minExclusive, minInclusive, maxExclusive, maxInclusive, minLength, maxLength);

            } else if (expectedNodeKind.equals(SHACLM.BlankNode)) {
                return ResourceFactory.createResource();
            }
            else if (expectedNodeKind.equals(SHACLM.BlankNodeOrIRI)) {
                // Generate either a random IRI or a random blank node
                if (new Random().nextBoolean()) {
                    return ResourceFactory.createResource("http://example.org/" + generateRandomString(pathName,minLength,maxLength));
                } else {
                    return ResourceFactory.createResource();
                }
            } else if (expectedNodeKind.equals(SHACLM.BlankNodeOrLiteral)) {
                // Generate either a random literal or a random blank node
                if (new Random().nextBoolean()) {
                    return generateLiteralValue(pathName,datatypeConstraint, minExclusive, minInclusive, maxExclusive, maxInclusive, minLength, maxLength);
                } else {
                    return ResourceFactory.createResource();
                }
            } else if (expectedNodeKind.equals(SHACLM.IRIOrLiteral)) {
                // Generate either a random IRI or a random literal
                if (new Random().nextBoolean()) {
                    return ResourceFactory.createResource("http://example.org/" + generateRandomString(pathName,minLength,maxLength));
                } else {
                    return generateLiteralValue(pathName,datatypeConstraint, minExclusive, minInclusive, maxExclusive, maxInclusive, minLength, maxLength);
                }
            }
        }

        if (propertyResource.getProperty(SHACLM.pattern) != null) {
            Literal propertyPattern = propertyResource.getProperty(SHACLM.pattern).getObject().asLiteral();
            String generatedValueByPattern = generateValueMatchingPattern(propertyPattern.getString());

            return ResourceFactory.createPlainLiteral(generatedValueByPattern);
        } else if (datatypeConstraint != null) {
            return generateLiteralValue(pathName,datatypeConstraint, minExclusive, minInclusive, maxExclusive, maxInclusive, minLength, maxLength);
        }

        //return null;
        return generateLiteralValue(pathName,datatypeConstraint, minExclusive, minInclusive, maxExclusive, maxInclusive, minLength, maxLength);


    }

    private static RDFNode generateLiteralValue(String pathName,RDFNode datatypeConstraint, Literal minExclusive, Literal minInclusive, Literal maxExclusive, Literal maxInclusive, Literal minLength, Literal maxLength) {
        if (datatypeConstraint != null) {
            String datatypeUri = datatypeConstraint.asResource().getURI();
            if (datatypeUri.equals(XSDDatatype.XSDdate.getURI())) {
                return ResourceFactory.createTypedLiteral(generateRandomDate(minExclusive, minInclusive, maxExclusive, maxInclusive), XSDDatatype.XSDdate);
            } else if (datatypeUri.equals(XSDDatatype.XSDinteger.getURI())) {
                int generatedRandom = generateRandomInteger(minExclusive, minInclusive, maxExclusive, maxInclusive);
                return ResourceFactory.createTypedLiteral(String.valueOf(generatedRandom), XSDDatatype.XSDinteger);
            } else if (datatypeUri.equals(XSDDatatype.XSDstring.getURI())) {
                return ResourceFactory.createTypedLiteral(generateRandomString(pathName,minLength, maxLength), XSDDatatype.XSDstring);
            }
            else if(datatypeUri.equals(XSDDatatype.XSDduration.getURI())){
                String randomDuration = generateRandomDuration();
                return ResourceFactory.createTypedLiteral(randomDuration, XSDDatatype.XSDduration);
            }
            else if(datatypeUri.equals(XSDDatatype.XSDboolean.getURI())){
                Random random = new Random();
                return ResourceFactory.createTypedLiteral(random.nextBoolean() ? "true" : "false", XSDDatatype.XSDboolean);
            }
            else if(datatypeUri.equals(XSDDatatype.XSDdecimal.getURI())){
                double randomDecimal = generateRandomDecimal(minExclusive, minInclusive, maxExclusive, maxInclusive);
                return ResourceFactory.createTypedLiteral(String.valueOf(randomDecimal), XSDDatatype.XSDdecimal);

            }
        }

        // If no datatype constraint is specified, return a default literal
        return ResourceFactory.createStringLiteral(generateRandomString(pathName,minLength, maxLength));
    }

    private static String generateRandomDuration() {
        Random random = new Random();

        int years = random.nextInt(10); // Generate a random number of years (0-9)
        int months = random.nextInt(12); // Generate a random number of months (0-11)
        int days = random.nextInt(30); // Generate a random number of days (0-29)
        int hours = random.nextInt(24); // Generate a random number of hours (0-23)
        int minutes = random.nextInt(60); // Generate a random number of minutes (0-59)
        int seconds = random.nextInt(60); // Generate a random number of seconds (0-59)

        return String.format("P%dY%dM%dDT%dH%dM%dS", years, months, days, hours, minutes, seconds);
    }



    private static String generateRandomString(String pathName, Literal minLength, Literal maxLength) {
        int defaultMinLength = 0;
        int defaultMaxLength = 15;

        int minLengthValue = minLength != null ? Integer.parseInt(minLength.getString()) : defaultMinLength;
        int maxLengthValue = maxLength != null ? Integer.parseInt(maxLength.getString()) : defaultMaxLength;

        if (maxLengthValue < minLengthValue) {
            throw new IllegalArgumentException("maxLength cannot be less than minLength");
        }

        Faker faker = new Faker();
        String randomWord ="";
        pathName = pathName.toLowerCase();
        //dodadeno za PathName
        if(pathName.equals("surname") || pathName.equals("lastname")){
            randomWord = faker.name().lastName();
        }
        else if(pathName.equals("name") || pathName.equals("firstname")){
            randomWord = faker.name().firstName();
        }
        else if(pathName.contains("name")){
            randomWord = faker.name().username();
        }
        else if(pathName.contains("address")){
            randomWord = faker.address().streetAddress();
        }
        else if(pathName.contains("postalcode")){
            randomWord = faker.address().zipCode();
        }
        else if(pathName.contains("author")){
            randomWord = faker.book().author();
        }
        else if(pathName.contains("booktitle") || pathName.contains("title") ){
            randomWord = faker.book().title();
        }
        else if(pathName.contains("musicgenre")){
            randomWord = faker.music().genre();
        }
        else if(pathName.contains("bookgenre") || pathName.contains("genre")){
            randomWord = faker.book().genre();
        }

        else if(pathName.contains("artist")){
            randomWord = faker.artist().name();
        }

        else {
            // Generate a random word using Faker
            randomWord = faker.lorem().word();
        }

        // Replace dots with spaces
        randomWord = randomWord.replaceAll("\\.", " ");

        // Trim the word to ensure it doesn't exceed maxLength
        if (randomWord.length() > maxLengthValue) {
            randomWord = randomWord.substring(0, maxLengthValue);
        }

        return randomWord;
    }


    private static String generateValueMatchingPattern(String pattern) {
        Faker faker = new Faker();
        String generatedEmail = faker.internet().emailAddress();

        // Check if the generated email matches the pattern
        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(generatedEmail);
        if (matcher.matches()) {
            return generatedEmail;
        } else {
            try {
                Generex generex = new Generex(pattern);
                String generatedValue = generex.random();
                return generatedValue;
            } catch (Exception e) {
                e.printStackTrace();
                return "Default Value"; // Handle errors gracefully and fall back to default
            }
        }

    }


private static String generateRandomDate(Literal minExclusive, Literal minInclusive, Literal maxExclusive, Literal maxInclusive) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

    Date minExclusiveDate = parseXsdDate(minExclusive);
    Date minInclusiveDate = parseXsdDate(minInclusive);
    Date maxExclusiveDate = parseXsdDate(maxExclusive);
    Date maxInclusiveDate = parseXsdDate(maxInclusive);

    if ((minExclusiveDate != null && minInclusiveDate != null && minExclusiveDate.after(minInclusiveDate)) ||
            (maxExclusiveDate != null && maxInclusiveDate != null && maxExclusiveDate.before(maxInclusiveDate))) {
        throw new IllegalArgumentException("Invalid date constraints.");
    }

    // Generate a random date within the allowed range
    Date randomDate = getRandomDateWithinRange(minExclusiveDate, minInclusiveDate, maxExclusiveDate, maxInclusiveDate);

    return sdf.format(randomDate);
}

    private static Date parseXsdDate(Literal dateLiteral) {
        if (dateLiteral != null && dateLiteral.getDatatype() != null
                && dateLiteral.getDatatype().getURI().equals(XSDDatatype.XSDdate.getURI())) {
            try {
                String dateString = dateLiteral.getString();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                return sdf.parse(dateString);
            } catch (ParseException | DatatypeFormatException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static Date getRandomDateWithinRange(Date minExclusive, Date minInclusive, Date maxExclusive, Date maxInclusive) {
        Faker faker = new Faker();
        Date range1, range2;

        // Define the lower boundary (range1) based on constraints
        if (minInclusive != null) {
            range1 = minInclusive;
        } else if (minExclusive != null) {
            range1 = minExclusive;
        } else {
            range1 = faker.date().past(365, TimeUnit.DAYS); // Default to a random past date if no min constraints
        }
        // Define the upper boundary (range2) based on constraints
        if (maxInclusive != null) {
            range2 = maxInclusive;
        } else if (maxExclusive != null) {
            range2 = maxExclusive;
        } else {
            range2 = faker.date().future(365, TimeUnit.DAYS); // Default to a random future date if no max constraints
        }

        // Generate a random date within the defined range
        return faker.date().between(range1, range2);
    }



    private static int generateRandomInteger(Literal minExclusive, Literal minInclusive, Literal maxExclusive, Literal maxInclusive) {
        Faker faker = new Faker();
        Random random = new Random();

        int min = Integer.MIN_VALUE;
        int max =1000;// Integer.MAX_VALUE;// default dr vrednost?

        // Parse constraint values if provided
        if (minExclusive != null) {
            min = Math.max(min, minExclusive.getInt());
        }
        if (minInclusive != null) {
            min = Math.max(min, minInclusive.getInt() - 1);
        }
        if (maxExclusive != null) {
            max = Math.min(max, maxExclusive.getInt());
        }
        if (maxInclusive != null) {
            max = Math.min(max, maxInclusive.getInt() + 1);
        }

        // Ensure min is less than or equal to max
        if (min > max) {
            throw new IllegalArgumentException("Invalid constraints for generating random integer.");
        }

        int generatedRandom = faker.number().numberBetween(min, max);
        return generatedRandom;
    }

    private static double generateRandomDecimal(Literal minExclusive, Literal minInclusive, Literal maxExclusive, Literal maxInclusive) {
        Faker faker = new Faker();
        Random random = new Random();

        long minValue = Long.MIN_VALUE;
        long maxValue = 1000; //Long.MAX_VALUE

        // Parse constraint values if provided
        if (minExclusive != null) {
            minValue = minExclusive.getLong();
        }
        if (minInclusive != null) {
            minValue = minInclusive.getLong() - 1;
        }
        if (maxExclusive != null) {
            maxValue = maxExclusive.getLong();
        }
        if (maxInclusive != null) {
            maxValue = maxInclusive.getLong() + 1;
        }

        // Ensure min is less than or equal to max
        if (minValue > maxValue) {
            throw new IllegalArgumentException("Invalid constraints for generating random decimal.");
        }

        double randomDecimal = faker.number().randomDouble(2,minValue,maxValue);
        System.out.println(randomDecimal);
        return randomDecimal;
    }



}
