import * as React from 'react';

// src/main/resources/META-INF/resources/js/App.tsx
import { useState as useState4 } from "react";

// src/main/resources/META-INF/resources/js/config/constants.ts
var ENTITY_TYPES = {
  BLOGS: "BLOGS",
  CATEGORY: "CATEGORY",
  COMPANY: "COMPANY",
  DOCUMENTS: "DOC",
  MB: "MB",
  ORGANIZATION: "ORG",
  PAGES: "PAGES",
  SITES: "SITES",
  USERS: "USERS",
  WCM: "WCM"
};
var ENTITY_LABELS = {
  BLOGS: "blogs",
  CATEGORY: "categories",
  COMPANY: "company",
  DOC: "documents",
  MB: "message-boards",
  ORG: "organizations",
  PAGES: "pages",
  SITES: "sites",
  USERS: "users",
  WCM: "web-content"
};
var ENTITY_ICONS = {
  BLOGS: "blogs",
  CATEGORY: "categories",
  COMPANY: "briefcase",
  DOC: "documents-and-media",
  MB: "message-boards",
  ORG: "organizations",
  PAGES: "page",
  SITES: "sites",
  USERS: "user",
  WCM: "web-content"
};

// src/main/resources/META-INF/resources/js/config/entities.ts
var ORGANIZATION_CONFIG = {
  actionURL: "/ldf/org",
  entityType: ENTITY_TYPES.ORGANIZATION,
  fields: [
    {
      label: "number-of-organizations",
      name: "numberOfOrganizations",
      required: true,
      type: "number",
      validators: [
        { message: "please-enter-a-valid-number", type: "digits" },
        { message: "value-must-be-greater-than-0", type: "min", value: 1 }
      ]
    },
    {
      label: "base-organization-name",
      name: "baseOrganizationName",
      required: true,
      type: "text"
    },
    {
      advanced: false,
      dataSource: "/ldf/data/organizations",
      defaultValue: "0",
      label: "parent-organization",
      name: "parentOrganizationId",
      required: false,
      type: "select"
    },
    {
      advanced: false,
      defaultValue: false,
      label: "create-organization-site",
      name: "organizationSiteCreate",
      required: false,
      type: "toggle"
    }
  ],
  helpText: "organization-help-text",
  icon: "organizations",
  label: "organizations"
};
var ENTITY_CONFIGS = {
  [ENTITY_TYPES.ORGANIZATION]: ORGANIZATION_CONFIG
};
function getEntityConfig(entityType) {
  return ENTITY_CONFIGS[entityType];
}

// src/main/resources/META-INF/resources/js/hooks/useFormState.ts
import { useCallback, useReducer } from "react";

// src/main/resources/META-INF/resources/js/utils/validation.ts
function validateField(value, field) {
  if (field.required && !value.trim()) {
    return Liferay.Language.get("this-field-is-required");
  }
  if (field.validators) {
    for (const validator of field.validators) {
      const error = runValidator(value, validator);
      if (error) {
        return error;
      }
    }
  }
  return null;
}
function runValidator(value, validator) {
  switch (validator.type) {
    case "digits":
      if (value && !/^\d+$/.test(value)) {
        return validator.message;
      }
      break;
    case "min":
      if (validator.value !== void 0 && Number(value) < validator.value) {
        return validator.message;
      }
      break;
    case "max":
      if (validator.value !== void 0 && Number(value) > validator.value) {
        return validator.message;
      }
      break;
    case "required":
      if (!value.trim()) {
        return validator.message;
      }
      break;
  }
  return null;
}
function validateForm(values, fields) {
  const errors = {};
  for (const field of fields) {
    const error = validateField(values[field.name] || "", field);
    if (error) {
      errors[field.name] = error;
    }
  }
  return errors;
}

// src/main/resources/META-INF/resources/js/hooks/useFormState.ts
function formReducer(state, action) {
  switch (action.type) {
    case "SET_VALUE":
      return {
        ...state,
        errors: { ...state.errors, [action.field]: "" },
        values: { ...state.values, [action.field]: action.value }
      };
    case "SET_ERRORS":
      return { ...state, errors: action.errors };
    case "START_SUBMIT":
      return { ...state, submitting: true };
    case "END_SUBMIT":
      return { ...state, submitting: false };
    case "RESET":
      return { errors: {}, submitting: false, values: {} };
    default:
      return state;
  }
}
function useFormState(fields) {
  const initialValues = {};
  for (const field of fields) {
    if (field.defaultValue !== void 0) {
      initialValues[field.name] = String(field.defaultValue);
    }
  }
  const [state, dispatch] = useReducer(formReducer, {
    errors: {},
    submitting: false,
    values: initialValues
  });
  const setValue = useCallback((field, value) => {
    dispatch({ field, type: "SET_VALUE", value });
  }, []);
  const validate = useCallback(() => {
    const errors = validateForm(state.values, fields);
    dispatch({ errors, type: "SET_ERRORS" });
    return Object.keys(errors).length === 0;
  }, [fields, state.values]);
  const startSubmit = useCallback(() => {
    dispatch({ type: "START_SUBMIT" });
  }, []);
  const endSubmit = useCallback(() => {
    dispatch({ type: "END_SUBMIT" });
  }, []);
  const reset = useCallback(() => {
    dispatch({ type: "RESET" });
  }, []);
  return {
    ...state,
    endSubmit,
    reset,
    setValue,
    startSubmit,
    validate
  };
}

// src/main/resources/META-INF/resources/js/utils/api.ts
async function fetchResource(resourceURL, params) {
  const url = new URL(resourceURL, window.location.origin);
  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      url.searchParams.append(key, value);
    });
  }
  try {
    const response = await fetch(url.toString(), {
      credentials: "include",
      method: "GET"
    });
    const data = await response.json();
    if (data.error) {
      return { error: data.error, success: false };
    }
    return { data, success: true };
  } catch (error) {
    return {
      error: error instanceof Error ? error.message : "Unknown error",
      success: false
    };
  }
}
async function submitResource(resourceURL, params) {
  const url = new URL(resourceURL, window.location.origin);
  Object.entries(params).forEach(([key, value]) => {
    url.searchParams.append(key, value);
  });
  try {
    const response = await fetch(url.toString(), {
      credentials: "include",
      method: "GET"
    });
    const data = await response.json();
    if (data.error) {
      return { error: data.error, success: false };
    }
    return { data, success: true };
  } catch (error) {
    return {
      error: error instanceof Error ? error.message : "Unknown error",
      success: false
    };
  }
}

// src/main/resources/META-INF/resources/js/components/AdvancedOptions.tsx
import { useState } from "react";
function AdvancedOptions({ children }) {
  const [expanded, setExpanded] = useState(false);
  return /* @__PURE__ */ React.createElement("div", { className: "sheet-section" }, /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn btn-link btn-sm",
      onClick: () => setExpanded(!expanded),
      type: "button"
    },
    Liferay.Language.get("advanced-options"),
    /* @__PURE__ */ React.createElement("span", { className: `ml-2 icon-${expanded ? "minus" : "plus"}` })
  ), expanded && /* @__PURE__ */ React.createElement("div", { className: "mt-3" }, children));
}
var AdvancedOptions_default = AdvancedOptions;

// src/main/resources/META-INF/resources/js/hooks/useApiData.ts
import { useCallback as useCallback2, useEffect, useState as useState2 } from "react";
function useApiData(resourceURL, dataSource) {
  const [data, setData] = useState2([]);
  const [loading, setLoading] = useState2(false);
  const [error, setError] = useState2(null);
  const load = useCallback2(async () => {
    if (!resourceURL || !dataSource) {
      return;
    }
    setLoading(true);
    setError(null);
    const result = await fetchResource(resourceURL, {
      type: dataSource.split("/").pop() || ""
    });
    if (result.success && result.data) {
      setData(result.data);
    } else {
      setError(result.error || "Failed to load data");
    }
    setLoading(false);
  }, [resourceURL, dataSource]);
  useEffect(() => {
    load();
  }, [load]);
  return { data, error, loading, reload: load };
}

// src/main/resources/META-INF/resources/js/components/FormField.tsx
function FormField({ error, field, onChange, options, value }) {
  if (field.type === "toggle") {
    return /* @__PURE__ */ React.createElement("div", { className: "form-group" }, /* @__PURE__ */ React.createElement("label", { className: "toggle-switch", htmlFor: field.name }, /* @__PURE__ */ React.createElement(
      "input",
      {
        checked: value === "true",
        className: "toggle-switch-check",
        id: field.name,
        onChange: (e) => onChange(field.name, String(e.target.checked)),
        type: "checkbox"
      }
    ), /* @__PURE__ */ React.createElement("span", { "aria-hidden": "true", className: "toggle-switch-bar" }, /* @__PURE__ */ React.createElement("span", { className: "toggle-switch-handle" })), /* @__PURE__ */ React.createElement("span", { className: "toggle-switch-text" }, Liferay.Language.get(field.label))));
  }
  if (field.type === "select") {
    return /* @__PURE__ */ React.createElement("div", { className: `form-group ${error ? "has-error" : ""}` }, /* @__PURE__ */ React.createElement("label", { htmlFor: field.name }, Liferay.Language.get(field.label), field.required && /* @__PURE__ */ React.createElement("span", { className: "reference-mark text-warning" }, "*")), /* @__PURE__ */ React.createElement(
      "select",
      {
        className: "form-control",
        id: field.name,
        onChange: (e) => onChange(field.name, e.target.value),
        value
      },
      /* @__PURE__ */ React.createElement("option", { value: "" }, Liferay.Language.get("select")),
      (options || field.options || []).map((opt) => /* @__PURE__ */ React.createElement("option", { key: opt.value, value: opt.value }, opt.label))
    ), error && /* @__PURE__ */ React.createElement("div", { className: "form-feedback-item" }, error));
  }
  return /* @__PURE__ */ React.createElement("div", { className: `form-group ${error ? "has-error" : ""}` }, /* @__PURE__ */ React.createElement("label", { htmlFor: field.name }, Liferay.Language.get(field.label), field.required && /* @__PURE__ */ React.createElement("span", { className: "reference-mark text-warning" }, "*")), /* @__PURE__ */ React.createElement(
    "input",
    {
      className: "form-control",
      id: field.name,
      onChange: (e) => onChange(field.name, e.target.value),
      type: field.type === "number" ? "number" : "text",
      value
    }
  ), error && /* @__PURE__ */ React.createElement("div", { className: "form-feedback-item" }, error));
}
var FormField_default = FormField;

// src/main/resources/META-INF/resources/js/components/DynamicSelect.tsx
function DynamicSelect({ dataResourceURL, error, field, onChange, value }) {
  const { data, loading } = useApiData(dataResourceURL, field.dataSource);
  if (loading) {
    return /* @__PURE__ */ React.createElement("div", { className: "form-group" }, /* @__PURE__ */ React.createElement("label", { htmlFor: field.name }, Liferay.Language.get(field.label)), /* @__PURE__ */ React.createElement("div", { className: "loading-animation loading-animation-sm" }));
  }
  return /* @__PURE__ */ React.createElement(
    FormField_default,
    {
      error,
      field,
      onChange,
      options: data,
      value
    }
  );
}
var DynamicSelect_default = DynamicSelect;

// src/main/resources/META-INF/resources/js/components/ProgressBar.tsx
function ProgressBar({ percent, running }) {
  if (!running && percent === 0) {
    return null;
  }
  return /* @__PURE__ */ React.createElement("div", { className: "sheet-section" }, /* @__PURE__ */ React.createElement("div", { className: "progress" }, /* @__PURE__ */ React.createElement(
    "div",
    {
      className: "progress-bar",
      role: "progressbar",
      style: { width: `${percent}%` }
    },
    percent > 0 && `${Math.round(percent)}%`
  )));
}
var ProgressBar_default = ProgressBar;

// src/main/resources/META-INF/resources/js/components/ResultAlert.tsx
import { useEffect as useEffect2 } from "react";
function ResultAlert({ message, onDismiss, type }) {
  useEffect2(() => {
    if (message && type === "success") {
      const timer = setTimeout(onDismiss, 5e3);
      return () => clearTimeout(timer);
    }
  }, [message, onDismiss, type]);
  if (!message) {
    return null;
  }
  return /* @__PURE__ */ React.createElement("div", { className: `alert alert-${type} alert-dismissible`, role: "alert" }, /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "close",
      onClick: onDismiss,
      type: "button"
    },
    /* @__PURE__ */ React.createElement("span", { "aria-hidden": "true" }, "\xD7")
  ), message);
}
var ResultAlert_default = ResultAlert;

// src/main/resources/META-INF/resources/js/components/EntityForm.tsx
import { useState as useState3 } from "react";
function EntityForm({ actionResourceURL, config, dataResourceURL, progressResourceURL }) {
  const { endSubmit, errors, reset, setValue, startSubmit, submitting, validate, values } = useFormState(config.fields);
  const [result, setResult] = useState3(null);
  const [progress, setProgress] = useState3({ percent: 0, running: false });
  const requiredFields = config.fields.filter((f) => !f.advanced);
  const advancedFields = config.fields.filter((f) => f.advanced);
  const handleSubmit = async () => {
    if (!validate()) {
      return;
    }
    startSubmit();
    setResult(null);
    const response = await submitResource(actionResourceURL, values);
    endSubmit();
    if (response.success) {
      setResult({
        message: Liferay.Language.get("execution-completed-successfully"),
        type: "success"
      });
    } else {
      setResult({
        message: response.error || Liferay.Language.get("an-error-occurred"),
        type: "danger"
      });
    }
  };
  const renderField = (field) => {
    if (field.dataSource) {
      return /* @__PURE__ */ React.createElement(
        DynamicSelect_default,
        {
          dataResourceURL,
          error: errors[field.name],
          field,
          key: field.name,
          onChange: setValue,
          value: values[field.name] || ""
        }
      );
    }
    return /* @__PURE__ */ React.createElement(
      FormField_default,
      {
        error: errors[field.name],
        field,
        key: field.name,
        onChange: setValue,
        value: values[field.name] || ""
      }
    );
  };
  return /* @__PURE__ */ React.createElement("div", { className: "sheet sheet-lg" }, /* @__PURE__ */ React.createElement("div", { className: "sheet-header" }, /* @__PURE__ */ React.createElement("h2", null, Liferay.Language.get(config.label))), /* @__PURE__ */ React.createElement("div", { className: "sheet-section" }, requiredFields.map(renderField), advancedFields.length > 0 && /* @__PURE__ */ React.createElement(AdvancedOptions_default, null, advancedFields.map(renderField))), /* @__PURE__ */ React.createElement(ProgressBar_default, { percent: progress.percent, running: progress.running }), /* @__PURE__ */ React.createElement("div", { className: "sheet-footer" }, /* @__PURE__ */ React.createElement(
    "button",
    {
      className: "btn btn-primary",
      disabled: submitting,
      onClick: handleSubmit,
      type: "button"
    },
    submitting ? Liferay.Language.get("running") : Liferay.Language.get("run")
  )), result && /* @__PURE__ */ React.createElement(
    ResultAlert_default,
    {
      message: result.message,
      onDismiss: () => setResult(null),
      type: result.type
    }
  ));
}
var EntityForm_default = EntityForm;

// src/main/resources/META-INF/resources/js/components/EntitySelector.tsx
var ENTITY_LIST = Object.values(ENTITY_TYPES);
function EntitySelector({ onSelect, selected }) {
  return /* @__PURE__ */ React.createElement("div", { className: "card-page card-page-equal-height" }, ENTITY_LIST.map((entityType) => /* @__PURE__ */ React.createElement("div", { className: "card-page-item col-md-3 col-sm-6", key: entityType }, /* @__PURE__ */ React.createElement(
    "div",
    {
      className: `card card-interactive card-interactive-primary ${selected === entityType ? "active" : ""}`,
      onClick: () => onSelect(entityType),
      role: "button",
      tabIndex: 0
    },
    /* @__PURE__ */ React.createElement("div", { className: "card-body" }, /* @__PURE__ */ React.createElement("div", { className: "card-row" }, /* @__PURE__ */ React.createElement("div", { className: "autofit-col" }, /* @__PURE__ */ React.createElement("span", { className: "sticker sticker-primary" }, /* @__PURE__ */ React.createElement("svg", { className: "lexicon-icon" }, /* @__PURE__ */ React.createElement(
      "use",
      {
        xlinkHref: `${Liferay.ThemeDisplay.getPathThemeImages?.() || ""}/clay/icons.svg#${ENTITY_ICONS[entityType]}`
      }
    )))), /* @__PURE__ */ React.createElement("div", { className: "autofit-col autofit-col-expand" }, /* @__PURE__ */ React.createElement("div", { className: "card-title" }, Liferay.Language.get(ENTITY_LABELS[entityType])))))
  ))));
}
var EntitySelector_default = EntitySelector;

// src/main/resources/META-INF/resources/js/App.tsx
function App({ actionResourceURL, dataResourceURL, progressResourceURL }) {
  const [selectedEntity, setSelectedEntity] = useState4(
    ENTITY_TYPES.ORGANIZATION
  );
  const entityConfig = getEntityConfig(selectedEntity);
  return /* @__PURE__ */ React.createElement("div", { className: "container-fluid container-fluid-max-xl" }, /* @__PURE__ */ React.createElement(
    EntitySelector_default,
    {
      onSelect: setSelectedEntity,
      selected: selectedEntity
    }
  ), entityConfig ? /* @__PURE__ */ React.createElement(
    EntityForm_default,
    {
      actionResourceURL,
      config: entityConfig,
      dataResourceURL,
      key: selectedEntity,
      progressResourceURL
    }
  ) : /* @__PURE__ */ React.createElement("div", { className: "sheet sheet-lg" }, /* @__PURE__ */ React.createElement("div", { className: "sheet-section" }, /* @__PURE__ */ React.createElement("div", { className: "alert alert-info" }, Liferay.Language.get("this-entity-type-is-not-yet-available")))));
}
var App_default = App;

// src/main/resources/META-INF/resources/js/Calculator.tsx
import { useState as useState5 } from "react";
function Calculator({ calculateURL }) {
  const [num1, setNum1] = useState5("");
  const [num2, setNum2] = useState5("");
  const [operator, setOperator] = useState5("+");
  const [result, setResult] = useState5(null);
  const [error, setError] = useState5(null);
  const [loading, setLoading] = useState5(false);
  const handleCalculate = () => {
    setLoading(true);
    setError(null);
    const url = calculateURL + "&num1=" + encodeURIComponent(num1) + "&num2=" + encodeURIComponent(num2) + "&operator=" + encodeURIComponent(operator);
    fetch(url, {
      credentials: "include",
      method: "GET"
    }).then((response) => response.json()).then((data) => {
      if (data.error) {
        setError(data.error);
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
  App_default as App,
  Calculator_default as Calculator
};
