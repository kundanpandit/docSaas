import "@/lib/zodConfig"; // must stay first: configures zod before any schema module loads
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

        <Toaster
          position="top-center"
          toastOptions={{
            className: "glass-strong rounded-xl! text-sm! text-foreground!",
            style: { background: "var(--glass-bg-strong)" },
          }}
        />
        
      </ReduxProvider>
    </GoogleOAuthProvider>
  </React.StrictMode>
);