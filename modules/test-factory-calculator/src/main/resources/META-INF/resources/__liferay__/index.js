import * as React from 'react';

// src/main/resources/META-INF/resources/js/Calculator.js
import { useState } from "react";
function Calculator() {
  const [num1, setNum1] = useState("");
  const [num2, setNum2] = useState("");
  const [operator, setOperator] = useState("+");
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const handleCalculate = () => {
    setLoading(true);
    setError(null);
    const formData = new FormData();
    formData.append("num1", num1);
    formData.append("num2", num2);
    formData.append("operator", operator);
    formData.append("serviceContext", JSON.stringify({}));
    fetch("/api/jsonws/TestFactory.CalcEntry/calculate", {
      body: formData,
      credentials: "include",
      headers: {
        "x-csrf-token": Liferay.authToken
      },
      method: "POST"
    }).then((response) => response.json()).then((data) => {
      if (data.exception) {
        setError(data.exception);
      } else {
        setResult(data.result);
      }
    }).catch((err) => {
      setError(err.message);
    }).finally(() => {
      setLoading(false);
    });
  };
  return /* @__PURE__ */ React.createElement("div", { className: "container-fluid container-fluid-max-xl" }, /* @__PURE__ */ React.createElement("div", { className: "sheet sheet-lg" }, /* @__PURE__ */ React.createElement("div", { className: "sheet-header" }, /* @__PURE__ */ React.createElement("h2", null, Liferay.Language.get("calculator"))), /* @__PURE__ */ React.createElement("div", { className: "sheet-section" }, /* @__PURE__ */ React.createElement("div", { className: "form-group" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "num1" }, Liferay.Language.get("number-1")), /* @__PURE__ */ React.createElement(
    "input",
    {
      className: "form-control",
      id: "num1",
      onChange: (e) => setNum1(e.target.value),
      type: "number",
      value: num1
    }
  )), /* @__PURE__ */ React.createElement("div", { className: "form-group" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "operator" }, Liferay.Language.get("operator")), /* @__PURE__ */ React.createElement(
    "select",
    {
      className: "form-control",
      id: "operator",
      onChange: (e) => setOperator(e.target.value),
      value: operator
    },
    /* @__PURE__ */ React.createElement("option", { value: "+" }, "+"),
    /* @__PURE__ */ React.createElement("option", { value: "-" }, "-"),
    /* @__PURE__ */ React.createElement("option", { value: "*" }, "\xD7"),
    /* @__PURE__ */ React.createElement("option", { value: "/" }, "\xF7")
  )), /* @__PURE__ */ React.createElement("div", { className: "form-group" }, /* @__PURE__ */ React.createElement("label", { htmlFor: "num2" }, Liferay.Language.get("number-2")), /* @__PURE__ */ React.createElement(
    "input",
    {
      className: "form-control",
      id: "num2",
      onChange: (e) => setNum2(e.target.value),
      type: "number",
      value: num2
    }
  )), /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn btn-primary",
      disabled: loading,
      onClick: handleCalculate,
      type: "button"
    },
    loading ? Liferay.Language.get("calculating") : Liferay.Language.get("calculate")
  )), result !== null && /* @__PURE__ */ React.createElement("div", { className: "sheet-footer" }, /* @__PURE__ */ React.createElement("div", { className: "alert alert-success" }, Liferay.Language.get("result"), ": ", result)), error && /* @__PURE__ */ React.createElement("div", { className: "sheet-footer" }, /* @__PURE__ */ React.createElement("div", { className: "alert alert-danger" }, error))));
}
var Calculator_default = Calculator;
export {
  Calculator_default as Calculator
};
