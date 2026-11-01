import { useEffect } from "react";

const setMetaDescription = (content) => {
  let tag = document.querySelector('meta[name="description"]');

  if (!tag) {
    tag = document.createElement("meta");
    tag.setAttribute("name", "description");
    document.head.appendChild(tag);
  }

  tag.setAttribute("content", content);
};

const usePageTitle = (title, description) => {
  useEffect(() => {
    document.title = title ? `${title} | DocSaaS` : "DocSaaS - Smart document tools";

    if (description) {
      setMetaDescription(description);
    }
  }, [title, description]);
};

export default usePageTitle;
