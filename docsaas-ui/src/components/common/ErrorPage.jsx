import { Link, useRouteError } from "react-router-dom";

const ErrorPage = () => {
  const error = useRouteError();

  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <h1 className="text-4xl font-bold">
        Something went wrong
      </h1>

      <p className="mt-4">
        {error?.statusText || error?.message}
      </p>

      <Link
        to="/"
        className="mt-6 underline"
      >
        Back Home
      </Link>
    </div>
  );
};

export default ErrorPage;