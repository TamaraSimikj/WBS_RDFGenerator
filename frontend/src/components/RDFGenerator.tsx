import React, { useState } from "react";
import {
  Button,
  CircularProgress,
  Container,
  FormControl,
  FormControlLabel,
  Paper,
  Radio,
  RadioGroup,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import axios from "axios";
import GetAppIcon from "@mui/icons-material/GetApp";

const RDFGenerator = () => {
  const [generatedRdfData, setGeneratedRdfData] = useState("");
  const [numExamples, setNumExamples] = useState("");
  const [shapePath, setShapePath] = useState("");
  const [shaclShape, setSHACLShape] = useState("");
  const [inputFile, setInputFile] = useState<File | null>(null);
  const [shapeType, setShapeType] = useState<string>("none");
  const [loading, setLoading] = useState(false);

  const handleGenerateData = () => {
    setGeneratedRdfData("");
    setLoading(true);
    if (shapeType === "filePath") {
      // Call API with shapePath and numExamples
      axios
        .post("http://localhost:8080/api/generateFromShapePath", {
          shapeFileName: shapePath,
          numberOfExamples: numExamples,
        })
        .then((response) => {
          console.log("responded, good connection");
          setGeneratedRdfData(response.data);
        })
        .catch((error) => {
          console.error("API Error:", error);
          setGeneratedRdfData("Error occurred while generating data.");
        })
        .finally(() => {
          setLoading(false);
        });
    } else if (shapeType === "file" && inputFile) {
      const formData = new FormData();
      formData.append("inputFile", inputFile);
      formData.append("numberOfExamples", numExamples);

      axios
        .post("http://localhost:8080/api/generateFromShapeWithInput", formData)
        .then((response) => {
          console.log("responded, good connection");
          setGeneratedRdfData(response.data);
        })
        .catch((error) => {
          console.error("API Error:", error);
          setGeneratedRdfData("Error occurred while generating data.");
        })
        .finally(() => {
          setLoading(false);
        });
    } else if (shapeType === "input") {
      axios
        .post("http://localhost:8080/api/generateFromShape", {
          shape: shaclShape,
          numberOfExamples: numExamples,
        })
        .then((response) => {
          console.log("responded, good connection");
          setGeneratedRdfData(response.data);
        })
        .catch((error) => {
          console.error("API Error:", error);
          setGeneratedRdfData("Error occurred while generating data.");
        })
        .finally(() => {
          setLoading(false);
        });
    } else {
      setGeneratedRdfData("Invalid option or missing input.");
    }
  };

  const handleFilterChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setShapeType(value);
    setGeneratedRdfData("");
  };

  const handleDownloadData = () => {
    axios
      .get("http://localhost:8080/api/downloadGeneratedData", {
        responseType: "blob",
      })
      .then((response) => {
        const url = window.URL.createObjectURL(new Blob([response.data]));
        const link = document.createElement("a");
        link.href = url;
        link.setAttribute("download", "generated_data.ttl");
        document.body.appendChild(link);
        link.click();
      })
      .catch((error) => {
        console.error("Error downloading data:", error);
      });
  };

  return (
    <Container component={Paper} sx={{ mt: 2 }}>
      <Typography align="center" variant="h3">
        RDF Data Generator
      </Typography>
      <Stack p={2}>
        <Typography variant="h6">get shape from:</Typography>
        <FormControl size="small">
          <RadioGroup
            row
            aria-labelledby="demo-row-radio-buttons-group-label"
            name="row-radio-buttons-group"
            onChange={handleFilterChange}
          >
            <FormControlLabel
              value="filePath"
              control={<Radio />}
              label="filePath"
            />
            <FormControlLabel value="file" control={<Radio />} label="file" />
            <FormControlLabel
              value="input"
              control={<Radio />}
              label="Input Shape"
            />
          </RadioGroup>
        </FormControl>
      </Stack>
      <Stack px={3}>
        <TextField
          label="Number of examples"
          value={numExamples}
          onChange={(e) => {
            setNumExamples(e.target.value);
          }}
          type="number"
          variant="outlined"
          margin="normal"
          sx={{ width: "20%" }}
          size="small"
        />
        {shapeType === "filePath" && (
          <TextField
            label="Shape Path"
            value={shapePath}
            onChange={(e) => {
              setShapePath(e.target.value);
            }}
            variant="outlined"
            margin="normal"
            size="small"
          />
        )}
        {shapeType === "file" && (
          <div>
            <input
              type="file"
              onChange={(e) => {
                if (e.target.files) {
                  setInputFile(e.target.files[0]);
                }
              }}
            />
          </div>
        )}
        {shapeType === "input" && (
          <TextField
            label="Shape Input"
            multiline
            rows={10}
            value={shaclShape}
            onChange={(e) => {
              setSHACLShape(e.target.value);
            }}
            variant="outlined"
            margin="normal"
            size="small"
          />
        )}

        <Button
          variant="contained"
          onClick={handleGenerateData}
          sx={{ width: "20%", mt: 2 }}
        >
          Generate
        </Button>
      </Stack>

      <Stack>
        <h2>Generated Data for this shape:</h2>
        {/* <pre>{generatedRdfData}</pre> */}
        {loading ? (
          // Render loading indicator (e.g., CircularProgress)
          <CircularProgress sx={{ mt: 2 }} />
        ) : (
          // Render the "Download Generated Data" button
          generatedRdfData && (
            <Button
              variant="contained"
              onClick={handleDownloadData}
              sx={{ mt: 2 }}
              startIcon={<GetAppIcon />}
            >
              Download Generated Data
            </Button>
          )
        )}
      </Stack>
    </Container>
  );
};

export default RDFGenerator;
