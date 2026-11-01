import { Link } from "react-router-dom";
import LoginForm from "../components/LoginForm";
import usePageTitle from "@/hooks/usePageTitle";

const LoginPage = () => {
  usePageTitle("Log in");
  return (
    <div className="w-full">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold tracking-tight">Welcome back</h1>
        <p className="mt-1.5 text-sm text-muted-foreground">Log in for 30 credits a day, AI tools and your job history.</p>
      </div>

      <LoginForm />

      <p className="mt-6 text-center text-sm text-muted-foreground">
        Don't have an account?{" "}
        <Link to="/register" className="font-medium text-primary hover:underline">
          Sign up free
        </Link>
      </p>
    </div>
  );
};

export default LoginPage;
