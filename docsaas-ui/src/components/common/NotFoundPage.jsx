import { Link } from "react-router-dom";

const NotFoundPage = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center">
      <h1 className="text-6xl font-bold">404</h1>

      <p className="mt-4 text-lg">
        Page not found
      </p>

      <Link
        to="/"
        className="mt-6 underline"
      >
        Go Home
      </Link>
    </div>
  );
};

export default NotFoundPage;