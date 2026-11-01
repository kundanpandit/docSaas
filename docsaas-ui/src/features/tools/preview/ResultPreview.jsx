import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";

import { apiClient } from "@/services/api/apiClient";
import DocumentPreview from "./DocumentPreview";

/** Fetches the finished result once and previews it, so users see the output before downloading. */
const ResultPreview = ({ job }) => {
  const [file, setFile] = useState(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;
    apiClient
      .get(`/v1/jobs/${job.id}/download`, { responseType: "blob" })
      .then((res) => active && setFile(new File([res.data], job.output.fileName, { type: job.output.mimeType })))
      .catch(() => active && setFailed(true));
    return () => {
      active = false;
    };
  }, [job.id, job.output.fileName, job.output.mimeType]);

  if (failed) return null;
  if (!file) {
    return (
      <div className="mt-6 flex h-24 items-center justify-center text-muted-foreground">
        <Loader2 className="animate-spin" />
      </div>
    );
  }
  return (
    <div className="mt-6 text-left">
      <DocumentPreview files={[file]} title="Result preview" />
    </div>
  );
};

export default ResultPreview;
