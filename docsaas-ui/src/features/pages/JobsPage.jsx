import usePageTitle from "@/hooks/usePageTitle";
import { RETENTION_MINUTES } from "@/features/home/constants/tools";
import { useGetMyJobsQuery } from "@/features/tools/api/toolsApi";
import JobList from "@/features/tools/components/JobList";

const JobsPage = () => {
  usePageTitle("Jobs");
  const { data: jobs, isLoading, isError } = useGetMyJobsQuery(undefined, { refetchOnMountOrArgChange: true });

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">Jobs</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Your last 50 jobs. Results can be downloaded for {RETENTION_MINUTES} minutes.
        </p>
      </div>
      <section className="glass rounded-3xl p-3 sm:p-4">
        <JobList jobs={jobs} isLoading={isLoading} isError={isError} />
      </section>
    </div>
  );
};

export default JobsPage;
