package com.example.rdfgenerator.model;

import lombok.Data;
import org.springframework.data.relational.core.sql.In;

@Data
public class FormShapeInput {
 String fileContent;
 Integer numberOfExamples;
}
