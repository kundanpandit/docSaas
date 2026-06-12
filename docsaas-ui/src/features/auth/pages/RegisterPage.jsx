import { Link } from "react-router-dom";
import RegisterForm from "../components/RegisterForm";

const RegisterPage = () => {
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
          Create Account 🚀
        </h2>

        <p className="mt-2 text-sm text-muted-foreground">
          Start using DocSaaS in less than a minute.
        </p>
      </div>

      <RegisterForm />

      {/* Footer Links */}
      <div className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link
          to="/login"
          className="font-medium text-blue-600 hover:text-blue-700"
        >
          Sign In
        </Link>
      </div>
    </div>
  );
};

export default RegisterPage;