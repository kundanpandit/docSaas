import { Link } from "react-router-dom";
import RegisterForm from "../components/RegisterForm";
import usePageTitle from "@/hooks/usePageTitle";

const RegisterPage = () => {
  usePageTitle("Create account");
  return (
    <div className="w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Create your account</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">Free forever. Takes less than a minute.</p>
      </div>

      <RegisterForm />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Already have an account?{" "}
        <Link to="/login" className="font-medium text-primary hover:underline">
          Log in
        </Link>
      </p>
    </div>
  );
};

export default RegisterPage;
