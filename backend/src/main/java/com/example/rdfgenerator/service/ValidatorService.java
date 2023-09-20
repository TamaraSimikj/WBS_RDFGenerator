package com.example.rdfgenerator.service;
import com.example.rdfgenerator.model.ValidationResult;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.vocabulary.SHACLM;
import org.springframework.stereotype.Service;

import java.io.StringReader;

@Service
public class ValidatorService {
    public ValidationResult validateRDF(String shape, String data) {
        // Basic input validation
        if (shape == null || data == null || shape.isEmpty() || data.isEmpty()) {
            return new ValidationResult("Invalid input: Please provide both RDF data and SHACL shape.", false);
        }

        Model dataModel = ModelFactory.createDefaultModel();
        dataModel.read(new StringReader(data), null, "TURTLE");

        Model shapeModel = ModelFactory.createDefaultModel();
        shapeModel.read(new StringReader(shape), null, "TURTLE");

        Shapes shapes = Shapes.parse(shapeModel);


        ValidationReport report = ShaclValidator.get().validate(shapes, dataModel.getGraph());

        if (report.conforms()) {
            return new ValidationResult("Data conforms with the shape", true);
        } else {
            StringBuilder validationResultBuilder = new StringBuilder();
            report.getModel().listStatements().forEachRemaining(stmt -> {
                if (stmt.getPredicate().equals(SHACLM.resultMessage)) {
                    validationResultBuilder
                            .append("Data doesn't conform with the shape, fail message")
                            .append(": ")
                            .append(stmt.getObject().toString())
                            .append("\n");
                }
            });

            String validationResult = validationResultBuilder.toString();
            return new ValidationResult(validationResult, false);
        }
    }
}
