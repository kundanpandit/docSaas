const MAX_PREVIEW_BYTES = 25 * 1024 * 1024;

/** Whether a job's output can be shown in the browser (PDF or image, not too large). */
export const canPreviewResult = (job) =>
  job?.output && /^(application\/pdf|image\/(png|jpeg|webp))/.test(job.output.mimeType) && job.output.sizeBytes <= MAX_PREVIEW_BYTES;
