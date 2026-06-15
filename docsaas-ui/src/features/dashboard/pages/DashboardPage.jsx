
import usePageTitle from "@/hooks/usePageTitle";

const DashboardPage = () => {

  usePageTitle("Dashboard");



  return (
    <div className="w-full">
      <h1 className="text-3xl font-bold">
        Dashboard
      </h1>

      <p className="mt-2 text-muted-foreground">
        Welcome to DocSaaS
      </p>
    </div>
  );
};

export default DashboardPage;