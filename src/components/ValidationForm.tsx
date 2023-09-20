import React, { useState } from "react";
import axios from "axios";
import Container from "@mui/material/Container";
import TextField from "@mui/material/TextField";
import Button from "@mui/material/Button";
import { Paper, Stack, Typography } from "@mui/material";

const ValidationForm = () => {
  const [rdfData, setRDFData] = useState("");
  const [shaclShapes, setSHACLShapes] = useState("");
  const [validationResult, setValidationResult] = useState("");

  const handleValidation = () => {
    // Make an API request to validate RDF data with SHACL shapes
    axios
      .post("http://localhost:8080/api/validate", {
        shape: shaclShapes,
        data: rdfData,
      })
      .then((response) => {
        // Update the validation result
        console.log("responded, good connection", response.data);
        setValidationResult(response.data.message);
      })
      .catch((error) => {
        // Handle API errors
        console.error("API Error:", error);
        setValidationResult("Error occurred while validating data.");
      });
  };

  return (
    <Container component={Paper} sx={{ mt: 2 }}>
      <Typography align="center" variant="h3">
        RDF Data Validator
      </Typography>

      <Stack direction={"row"} justifyContent={"space-between"}>
        <TextField
          sx={{ pr: 1 }}
          label="SHACL Shape"
          multiline
          rows={15}
          value={shaclShapes}
          onChange={(e) => {
            setSHACLShapes(e.target.value);
          }}
          variant="outlined"
          fullWidth
          margin="normal"
        />
        <TextField
          label="RDF Data"
          multiline
          rows={15}
          value={rdfData}
          onChange={(e) => {
            setRDFData(e.target.value);
          }}
          variant="outlined"
          fullWidth
          margin="normal"
        />
      </Stack>

      <Button variant="contained" onClick={handleValidation}>
        Validate
      </Button>
      <div>
        <h2>Validation Result:</h2>
        <pre>{validationResult}</pre>
      </div>
    </Container>
  );
};

export default ValidationForm;
