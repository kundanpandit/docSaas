import React from "react";
import ReactDOM from "react-dom/client";

import App from "./App";
import "./index.css";

import { Toaster } from "react-hot-toast";
import { GoogleOAuthProvider } from "@react-oauth/google";

import ReduxProvider from "@/providers/ReduxProvider";

ReactDOM.createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <GoogleOAuthProvider
      clientId={import.meta.env.VITE_GOOGLE_CLIENT_ID}
    >
      <ReduxProvider>
        <App />

        <Toaster />
        
      </ReduxProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);