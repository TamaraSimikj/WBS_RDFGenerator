import React from "react";
import { BrowserRouter as Router, Link, Route, Routes } from "react-router-dom";
import ValidationForm from "./components/ValidationForm";
import RDFGenerator from "./components/RDFGenerator";
import { Paper, Stack, Typography, styled } from "@mui/material";

const Item = styled(Paper)(({ theme }) => ({
  ...theme.typography.body2,
  textAlign: "center",
  color: theme.palette.text.secondary,
  height: 300,
  width: 300,
}));

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomeComponent />} />
        <Route path="/validator" element={<ValidationForm />} />
        <Route path="/generator" element={<RDFGenerator />} />
      </Routes>
    </Router>
  );
}

const HomeComponent = () => {
  return (
    <>
      <Stack
        pt={3}
        direction={"row"}
        margin={"auto"}
        justifyContent={"space-around"}
        width={"50%"}
      >
        <Item elevation={3}>
          <Link to="/validator" style={{ textDecoration: "none" }}>
            <Typography variant="h4" sx={{ margin: "30%" }}>
              RDF Validator
            </Typography>
          </Link>
        </Item>
        <Item elevation={3}>
          <Link to="/generator" style={{ textDecoration: "none" }}>
            <Typography variant="h4" sx={{ margin: "30%" }}>
              RDF Generator
            </Typography>
          </Link>
        </Item>
      </Stack>
    </>
  );
};

export default App;
