import { Link } from "react-router-dom";
import LoginForm from "../components/LoginForm";

const LoginPage = () => {
  return (
    <div className="w-full max-w-md">
      {/* Logo / Brand */}
      <div className="mb-10 text-center">
        <h1 className="text-4xl font-bold tracking-tight text-blue-600">
          DocSaaS
        </h1>

        <p className="mt-2 text-sm text-muted-foreground">
          Secure PDF tools for everyone
        </p>
      </div>

      {/* Auth Card Header */}
      <div className="mb-8">
        <h2 className="text-3xl font-bold">
          Welcome Back 👋
        </h2>

        <p className="mt-2 text-sm text-muted-foreground">
          Sign in to continue managing your documents.
        </p>
      </div>

      <LoginForm />

      {/* Footer Links */}
      <div className="mt-6 text-center text-sm text-muted-foreground">
        Don't have an account?{" "}
        <Link
          to="/register"
          className="font-medium text-blue-600 hover:text-blue-700"
        >
          Create Account
        </Link>
      </div>
    </div>
  );
};

export default LoginPage;