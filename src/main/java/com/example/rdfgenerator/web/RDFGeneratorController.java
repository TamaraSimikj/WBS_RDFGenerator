package com.example.rdfgenerator.web;

import com.example.rdfgenerator.model.*;
import com.example.rdfgenerator.service.GeneratorService;
import com.example.rdfgenerator.service.ValidatorService;
import org.apache.jena.rdf.model.Model;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api")
//@CrossOrigin(origins = "http://localhost:3000")
public class RDFGeneratorController {

    private final GeneratorService generatorService;
    private final ValidatorService validationService;

    public RDFGeneratorController(GeneratorService generatorService, ValidatorService validationService) {
        this.generatorService = generatorService;
        this.validationService = validationService;
    }

    @PostMapping("/generateFromShapePath")
    public ResponseEntity<Resource> generateDataFromShapePath(@RequestBody FormShapePath formShapePath) {
        String outputPath = generatorService.generateDataFileForShapePath(formShapePath.getShapeFileName(), formShapePath.getNumberOfExamples());

        if (outputPath != null) {
            try {
                Path filePath = Paths.get(outputPath);
                Resource resource = new FileSystemResource(filePath.toFile());

                return ResponseEntity
                        .ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                        .body(resource);
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/downloadGeneratedData")
    public ResponseEntity<FileSystemResource> downloadGeneratedData() {
        // Replace "path/to/generated_data.ttl" with the actual path to your generated file
        File generatedDataFile = new File("C:\\Users\\Tamara\\IdeaProjects\\RDFGenerator\\src\\main\\resources\\generatedData\\generated_data.ttl");

        if (!generatedDataFile.exists()) {
            // Handle the case where the file doesn't exist
            return ResponseEntity.notFound().build();
        }

        // Set the appropriate content type for the response
        MediaType mediaType = MediaType.parseMediaType("text/turtle");

        // Return the file as a ResponseEntity with content type and headers for download
        return ResponseEntity
                .ok()
                .contentType(mediaType)
                .header("Content-Disposition", "attachment; filename=generated_data.ttl")
                .body(new FileSystemResource(generatedDataFile));
    }
//    @PostMapping("/generateFromShapePath")
//    public List<String> generateDataFromShapePath(@RequestBody FormShapePath formShapePath) {
//        List<Model> generatedModels = generatorService.generateDataForShapePath(formShapePath.getShapeFileName(), formShapePath.getNumberOfExamples());
//
//        List<String> turtleStrings = new ArrayList<>();
//        for (Model model : generatedModels) {
//            StringWriter stringWriter = new StringWriter();
//            model.write(stringWriter, "Turtle");
//            turtleStrings.add(stringWriter.toString());
//        }
//
//        return turtleStrings;
//    }
@PostMapping("/generateFromShape")
public ResponseEntity<Resource> generateFromShape(@RequestBody FormShape formShape) {
    String outputPath  = generatorService.generateDataForShapeInput(formShape.getShape(), formShape.getNumberOfExamples());
    if (outputPath != null) {
        try {
            Path filePath = Paths.get(outputPath);
            Resource resource = new FileSystemResource(filePath.toFile());

            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } else {
        return ResponseEntity.badRequest().build();
    }
}

    @PostMapping("/generateFromShapeWithInput")
    public ResponseEntity<Resource> generateFromShapeWithInput(
            @RequestParam("inputFile") MultipartFile file,
            @RequestParam("numberOfExamples") Integer numberOfExamples
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            // Access the uploaded file data
            byte[] fileBytes = file.getBytes();
            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);

            // Now you have both fileContent and numberOfExamples
            String outputPath = generatorService.generateDataForShapeInput(fileContent, numberOfExamples);

            if (outputPath != null) {
                try {
                    Path filePath = Paths.get(outputPath);
                    Resource resource = new FileSystemResource(filePath.toFile());

                    if (resource.exists() && resource.isReadable()) {
                        return ResponseEntity
                                .ok()
                                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + resource.getFilename())
                                .body(resource);
                    } else {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }
            } else {
                return ResponseEntity.badRequest().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


//    @PostMapping("/generateFromShape")
//    public List<String> generateFromShape(@RequestBody FormShape formShape) {
//        List<Model> generatedModels = generatorService.generateDataForShapeInput(formShape.getShape(), formShape.getNumberOfExamples());
//
//        List<String> turtleStrings = new ArrayList<>();
//        for (Model model : generatedModels) {
//            StringWriter stringWriter = new StringWriter();
//            model.write(stringWriter, "++Turtle");
//            turtleStrings.add(stringWriter.toString());
//        }
//
//        return turtleStrings;
//    }

//    @PostMapping("/generateFromShapeWithInput")
//    public ResponseEntity<List<String>> generateFromShapeWithInput(
//            @RequestParam("inputFile") MultipartFile file,
//            @RequestParam("numberOfExamples") Integer numberOfExamples
//    ) {
//        try {
//            if (file.isEmpty()) {
//                return ResponseEntity.badRequest().body(Collections.singletonList("No file uploaded."));
//            }
//
//            // Access the uploaded file data
//            byte[] fileBytes = file.getBytes();
//            String fileContent = new String(fileBytes, StandardCharsets.UTF_8);
//
//            // Now you have both fileContent and numberOfExamples
//            List<Model> generatedModels = generatorService.generateDataForShapeInput(fileContent, numberOfExamples);
//
//            List<String> turtleStrings = new ArrayList<>();
//            for (Model model : generatedModels) {
//                StringWriter stringWriter = new StringWriter();
//                model.write(stringWriter, "Turtle");
//                turtleStrings.add(stringWriter.toString());
//            }
//
//            return ResponseEntity.ok(turtleStrings);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonList("An error occurred while processing the file."));
//        }
//    }


    @PostMapping("/validate")
    public ValidationResult validateRDF(@RequestBody ValidationForm validationForm) {
        return validationService.validateRDF(validationForm.getShape(), validationForm.getData());
    }


}
